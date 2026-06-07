package tachiyomi.presentation.core.components

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastLastOrNull
import androidx.compose.ui.util.fastMaxBy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample
import tachiyomi.presentation.core.components.Scroller.EXACT_HEIGHT_KEY_PREFIX
import tachiyomi.presentation.core.components.Scroller.STICKY_HEADER_KEY_PREFIX
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun VerticalFastScroller(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thumbAllowed: () -> Boolean = { true },
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    topContentPadding: Dp = Dp.Hairline,
    bottomContentPadding: Dp = Dp.Hairline,
    endContentPadding: Dp = Dp.Hairline,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val contentPlaceable = subcompose("content", content).map { it.measure(constraints) }
        val contentHeight = contentPlaceable.fastMaxBy { it.height }?.height ?: 0
        val contentWidth = contentPlaceable.fastMaxBy { it.width }?.width ?: 0

        val scrollerConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val scrollerPlaceable = subcompose("scroller") {
            // All scroll-state reads are delegated to ListFastScrollThumb so the
            // SubcomposeLayout measure pass never touches listState.layoutInfo
            // (which changes every frame during scroll, causing per-frame recomposition).
            ListFastScrollThumb(
                listState = listState,
                thumbAllowed = thumbAllowed,
                thumbColor = thumbColor,
                topContentPadding = topContentPadding,
                bottomContentPadding = bottomContentPadding,
                endContentPadding = endContentPadding,
                contentHeightPx = contentHeight,
            )
        }.map { it.measure(scrollerConstraints) }
        val scrollerWidth = scrollerPlaceable.fastMaxBy { it.width }?.width ?: 0

        layout(contentWidth, contentHeight) {
            contentPlaceable.fastForEach { it.place(0, 0) }
            scrollerPlaceable.fastForEach { it.placeRelative(contentWidth - scrollerWidth, 0) }
        }
    }
}

/**
 * The fast-scroll thumb for a [LazyListState].
 *
 * Extracted into its own composable so **zero** [LazyListState.layoutInfo] or
 * [LazyListState.isScrollInProgress] reads happen during composition (which would
 * cause a recomposition on every scroll frame inside the [SubcomposeLayout]).
 * All reactive logic runs inside [LaunchedEffect] + [snapshotFlow].
 *
 * ### Proportion math
 * `proportion = itemsBefore / (totalItems - viewportItems)`
 * where `itemsBefore` is the fractional count of items above the viewport (negative
 * first-item offset divided by avg item size gives sub-item precision).
 * This expression maps to exactly **0.0** at the top and **1.0** at the bottom —
 * fixing the "thumb never reaches the end" bug from the previous estimation approach.
 */
@Composable
private fun ListFastScrollThumb(
    listState: LazyListState,
    thumbAllowed: () -> Boolean,
    thumbColor: Color,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    endContentPadding: Dp,
    contentHeightPx: Int,
) {
    val density = LocalDensity.current

    val thumbTopPadding = remember(density, topContentPadding) {
        with(density) { topContentPadding.toPx() }
    }
    val thumbBottomPadding = remember(density, bottomContentPadding) {
        with(density) { bottomContentPadding.toPx() }
    }
    val thumbHeightPx = remember(density) { with(density) { ThumbLength.toPx() } }
    val trackHeightPx = remember(contentHeightPx, thumbTopPadding, thumbBottomPadding, thumbHeightPx) {
        (contentHeightPx - thumbTopPadding - thumbBottomPadding - thumbHeightPx).coerceAtLeast(0f)
    }

    var thumbOffsetY by remember(thumbTopPadding) { mutableFloatStateOf(thumbTopPadding) }

    val dragInteractionSource = remember { MutableInteractionSource() }
    val isThumbDragged by dragInteractionSource.collectIsDraggedAsState()

    val scrolled = remember {
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }

    // ── List → Thumb ─────────────────────────────────────────────────────────
    // Stable keys: this LaunchedEffect never restarts during an active scroll session.
    // snapshotFlow reads layoutInfo reactively on the coroutine thread — not composition.
    LaunchedEffect(listState, trackHeightPx, thumbTopPadding) {
        snapshotFlow {
            // While the thumb is being dragged it is the authority; ignore list position.
            if (isThumbDragged) return@snapshotFlow null

            val info = listState.layoutInfo
            val totalItems = info.totalItemsCount
            if (totalItems == 0 || info.visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val visibleItems = info.visibleItemsInfo
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()

            // If we are exactly at the top or bottom, return exact bounds to avoid calculation drift
            if (firstItem.index == 0 && firstItem.offset >= info.viewportStartOffset) {
                return@snapshotFlow 0f
            }
            if (lastItem.index == totalItems - 1 && lastItem.offset + lastItem.size <= info.viewportEndOffset) {
                return@snapshotFlow 1f
            }

            // Sub-item precision for the exact top index visible
            val topHiddenFraction = (info.viewportStartOffset - firstItem.offset).toFloat() / firstItem.size.coerceAtLeast(1)
            val exactTopIndex = firstItem.index.toFloat() + topHiddenFraction.coerceIn(0f, 1f)

            // Sub-item precision for the exact bottom index visible
            val bottomHiddenFraction = (lastItem.offset + lastItem.size - info.viewportEndOffset).toFloat() / lastItem.size.coerceAtLeast(1)
            val exactBottomIndex = lastItem.index.toFloat() - bottomHiddenFraction.coerceIn(0f, 1f)

            val totalVisible = (exactBottomIndex - exactTopIndex).coerceAtLeast(1f)
            val scrollableIndices = (totalItems - totalVisible).coerceAtLeast(1f)

            (exactTopIndex / scrollableIndices).coerceIn(0f, 1f)
        }.collectLatest { proportion ->
            if (proportion == null) return@collectLatest
            thumbOffsetY = trackHeightPx * proportion + thumbTopPadding
            if (listState.isScrollInProgress) scrolled.tryEmit(Unit)
        }
    }

    // ── Thumb → List ─────────────────────────────────────────────────────────
    // snapshotFlow emits null when not dragging → collectLatest returns early.
    // When dragging resumes it emits the current thumbOffsetY and scrolls instantly.
    LaunchedEffect(listState, trackHeightPx, thumbTopPadding) {
        snapshotFlow { if (isThumbDragged) thumbOffsetY else null }
            .collectLatest { y ->
                if (y == null) return@collectLatest

                val proportion = ((y - thumbTopPadding) / trackHeightPx).coerceIn(0f, 1f)

                val info = listState.layoutInfo
                val totalItems = info.totalItemsCount
                if (totalItems == 0) return@collectLatest

                val visibleItems = info.visibleItemsInfo
                if (visibleItems.isEmpty()) return@collectLatest

                val firstItem = visibleItems.first()
                val lastItem = visibleItems.last()
                
                val topHiddenFraction = (info.viewportStartOffset - firstItem.offset).toFloat() / firstItem.size.coerceAtLeast(1)
                val exactTopIndex = firstItem.index.toFloat() + topHiddenFraction.coerceIn(0f, 1f)
                
                val bottomHiddenFraction = (lastItem.offset + lastItem.size - info.viewportEndOffset).toFloat() / lastItem.size.coerceAtLeast(1)
                val exactBottomIndex = lastItem.index.toFloat() - bottomHiddenFraction.coerceIn(0f, 1f)
                
                val totalVisible = (exactBottomIndex - exactTopIndex).coerceAtLeast(1f)
                val scrollableIndices = (totalItems - totalVisible).coerceAtLeast(1f)

                val targetFractional = proportion * scrollableIndices
                val targetIndex = targetFractional.toInt().coerceIn(0, totalItems - 1)
                
                // We don't have exact item sizes for target, use avg visible size as fallback
                val avgItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size.coerceAtLeast(1)
                val targetOffset = ((targetFractional - targetIndex) * avgItemSize).roundToInt().coerceAtLeast(0)

                listState.scrollToItem(targetIndex, targetOffset)
                scrolled.tryEmit(Unit)
            }
    }

    // ── Visibility ───────────────────────────────────────────────────────────
    val alpha = remember { Animatable(0f) }
    val isThumbVisible = alpha.value > 0f
    LaunchedEffect(scrolled) {
        scrolled.sample(100).collectLatest {
            if (thumbAllowed()) {
                alpha.snapTo(1f)
                delay(ScrollBarVisibilityDurationMillis)
                alpha.animateTo(0f, animationSpec = ImmediateFadeOutAnimationSpec)
            } else {
                alpha.animateTo(0f, animationSpec = ImmediateFadeOutAnimationSpec)
            }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(0, thumbOffsetY.roundToInt()) }
            .then(
                if (isThumbVisible && !listState.isScrollInProgress) {
                    Modifier.draggable(
                        interactionSource = dragInteractionSource,
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            thumbOffsetY = (thumbOffsetY + delta).coerceIn(
                                thumbTopPadding,
                                thumbTopPadding + trackHeightPx,
                            )
                        },
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (isThumbVisible && !isThumbDragged && !listState.isScrollInProgress) {
                    Modifier.systemGestureExclusion()
                } else {
                    Modifier
                },
            )
            .height(ThumbLength)
            .padding(end = endContentPadding)
            .width(ThumbThickness)
            .alpha(alpha.value)
            .background(color = thumbColor, shape = ThumbShape),
    )
}

@Composable
fun VerticalGridFastScroller(
    state: LazyGridState,
    columns: GridCells,
    arrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    thumbAllowed: () -> Boolean = { true },
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    topContentPadding: Dp = Dp.Hairline,
    bottomContentPadding: Dp = Dp.Hairline,
    endContentPadding: Dp = Dp.Hairline,
    content: @Composable () -> Unit,
) {
    val slotSizesSums = rememberColumnWidthSums(
        columns = columns,
        horizontalArrangement = arrangement,
        contentPadding = contentPadding,
    )

    SubcomposeLayout(modifier = modifier) { constraints ->
        val contentPlaceable = subcompose("content", content).map { it.measure(constraints) }
        val contentHeight = contentPlaceable.fastMaxBy { it.height }?.height ?: 0
        val contentWidth = contentPlaceable.fastMaxBy { it.width }?.width ?: 0

        val scrollerConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val scrollerPlaceable = subcompose("scroller") {
            val layoutInfo = state.layoutInfo
            val showScroller = remember(columns, layoutInfo.totalItemsCount) {
                layoutInfo.visibleItemsInfo.size < layoutInfo.totalItemsCount
            }
            if (!showScroller) return@subcompose
            val thumbTopPadding = with(LocalDensity.current) { topContentPadding.toPx() }
            var thumbOffsetY by remember(thumbTopPadding) { mutableFloatStateOf(thumbTopPadding) }

            val dragInteractionSource = remember { MutableInteractionSource() }
            val isThumbDragged by dragInteractionSource.collectIsDraggedAsState()
            val scrolled = remember {
                MutableSharedFlow<Unit>(
                    extraBufferCapacity = 1,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST,
                )
            }

            val thumbBottomPadding = with(LocalDensity.current) { bottomContentPadding.toPx() }
            val heightPx = contentHeight.toFloat() -
                thumbTopPadding -
                thumbBottomPadding -
                state.layoutInfo.afterContentPadding
            val thumbHeightPx = with(LocalDensity.current) { ThumbLength.toPx() }
            val trackHeightPx = heightPx - thumbHeightPx

            val columnCount = remember(columns) { slotSizesSums(constraints).size.coerceAtLeast(1) }

            // ── Grid → Thumb ─────────────────────────────────────────────────────────
            LaunchedEffect(state, trackHeightPx, thumbTopPadding) {
                snapshotFlow {
                    if (isThumbDragged) return@snapshotFlow null

                    val info = state.layoutInfo
                    val totalItems = info.totalItemsCount
                    if (totalItems == 0 || info.visibleItemsInfo.isEmpty()) return@snapshotFlow null

                    val visibleItems = info.visibleItemsInfo
                    val firstItem = visibleItems.first()
                    val lastItem = visibleItems.last()

                    if (firstItem.index == 0 && firstItem.offset.y >= info.viewportStartOffset) {
                        return@snapshotFlow 0f
                    }
                    if (lastItem.index == totalItems - 1 && lastItem.offset.y + lastItem.size.height <= info.viewportEndOffset) {
                        return@snapshotFlow 1f
                    }

                    val itemsInTopRow = visibleItems.count { it.offset.y == firstItem.offset.y }
                    val topHiddenFraction = (info.viewportStartOffset - firstItem.offset.y).toFloat() / firstItem.size.height.coerceAtLeast(1)
                    val exactTopIndex = firstItem.index.toFloat() + topHiddenFraction.coerceIn(0f, 1f) * itemsInTopRow

                    val itemsInBottomRow = visibleItems.count { it.offset.y == lastItem.offset.y }
                    val bottomRowStartIndex = lastItem.index - itemsInBottomRow + 1
                    val bottomHiddenFraction = (lastItem.offset.y + lastItem.size.height - info.viewportEndOffset).toFloat() / lastItem.size.height.coerceAtLeast(1)
                    val exactBottomIndex = bottomRowStartIndex.toFloat() + (1f - bottomHiddenFraction.coerceIn(0f, 1f)) * itemsInBottomRow

                    val totalVisible = (exactBottomIndex - exactTopIndex).coerceAtLeast(1f)
                    val scrollableIndices = (totalItems - totalVisible).coerceAtLeast(1f)

                    (exactTopIndex / scrollableIndices).coerceIn(0f, 1f)
                }.collectLatest { proportion ->
                    if (proportion == null) return@collectLatest
                    thumbOffsetY = trackHeightPx * proportion + thumbTopPadding
                    if (state.isScrollInProgress) scrolled.tryEmit(Unit)
                }
            }

            // ── Thumb → Grid ─────────────────────────────────────────────────────────
            LaunchedEffect(state, trackHeightPx, thumbTopPadding, columnCount) {
                snapshotFlow { if (isThumbDragged) thumbOffsetY else null }
                    .collectLatest { y ->
                        if (y == null) return@collectLatest
                        
                        val proportion = ((y - thumbTopPadding) / trackHeightPx).coerceIn(0f, 1f)

                        val info = state.layoutInfo
                        val totalItems = info.totalItemsCount
                        if (totalItems == 0) return@collectLatest
                        
                        val visibleItems = info.visibleItemsInfo
                        if (visibleItems.isEmpty()) return@collectLatest

                        val firstItem = visibleItems.first()
                        val lastItem = visibleItems.last()

                        val itemsInTopRow = visibleItems.count { it.offset.y == firstItem.offset.y }
                        val topHiddenFraction = (info.viewportStartOffset - firstItem.offset.y).toFloat() / firstItem.size.height.coerceAtLeast(1)
                        val exactTopIndex = firstItem.index.toFloat() + topHiddenFraction.coerceIn(0f, 1f) * itemsInTopRow

                        val itemsInBottomRow = visibleItems.count { it.offset.y == lastItem.offset.y }
                        val bottomRowStartIndex = lastItem.index - itemsInBottomRow + 1
                        val bottomHiddenFraction = (lastItem.offset.y + lastItem.size.height - info.viewportEndOffset).toFloat() / lastItem.size.height.coerceAtLeast(1)
                        val exactBottomIndex = bottomRowStartIndex.toFloat() + (1f - bottomHiddenFraction.coerceIn(0f, 1f)) * itemsInBottomRow

                        val totalVisible = (exactBottomIndex - exactTopIndex).coerceAtLeast(1f)
                        val scrollableIndices = (totalItems - totalVisible).coerceAtLeast(1f)

                        val targetFractional = proportion * scrollableIndices
                        val targetIndex = targetFractional.toInt().coerceIn(0, totalItems - 1)

                        // Estimate offset based on average item height, similar to list fallback
                        val laidOutArea = (lastItem.offset.y + lastItem.size.height) - firstItem.offset.y
                        val laidOutRows = 1 + abs(lastItem.index - firstItem.index) / columnCount
                        val avgRowHeight = laidOutArea.toFloat() / laidOutRows.coerceAtLeast(1)
                        
                        val targetRowFraction = (targetFractional - targetIndex) / columnCount.coerceAtLeast(1)
                        val targetOffset = (targetRowFraction * avgRowHeight).roundToInt().coerceAtLeast(0)

                        state.scrollToItem(targetIndex, targetOffset)
                        scrolled.tryEmit(Unit)
                    }
            }

            val alpha = remember { Animatable(0f) }
            val isThumbVisible = alpha.value > 0f
            LaunchedEffect(scrolled, alpha) {
                scrolled
                    .sample(100)
                    .collectLatest {
                        if (thumbAllowed()) {
                            alpha.snapTo(1f)
                            delay(ScrollBarVisibilityDurationMillis)
                            alpha.animateTo(0f, animationSpec = ImmediateFadeOutAnimationSpec)
                        } else {
                            alpha.animateTo(0f, animationSpec = ImmediateFadeOutAnimationSpec)
                        }
                    }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffsetY.roundToInt()) }
                    .then(
                        if (isThumbVisible && !state.isScrollInProgress) {
                            Modifier.draggable(
                                interactionSource = dragInteractionSource,
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    val newOffsetY = thumbOffsetY + delta
                                    thumbOffsetY = newOffsetY.coerceIn(
                                        thumbTopPadding,
                                        thumbTopPadding + trackHeightPx,
                                    )
                                },
                            )
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (isThumbVisible && !isThumbDragged && !state.isScrollInProgress) {
                            Modifier.systemGestureExclusion()
                        } else {
                            Modifier
                        },
                    )
                    .height(ThumbLength)
                    .padding(end = endContentPadding)
                    .width(ThumbThickness)
                    .alpha(alpha.value)
                    .background(color = thumbColor, shape = ThumbShape),
            )
        }.map { it.measure(scrollerConstraints) }
        val scrollerWidth = scrollerPlaceable.fastMaxBy { it.width }?.width ?: 0

        layout(contentWidth, contentHeight) {
            contentPlaceable.fastForEach {
                it.place(0, 0)
            }
            scrollerPlaceable.fastForEach {
                it.placeRelative(contentWidth - scrollerWidth, 0)
            }
        }
    }
}

@Composable
private fun rememberColumnWidthSums(
    columns: GridCells,
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues,
) = remember<Density.(Constraints) -> List<Int>>(
    columns,
    horizontalArrangement,
    contentPadding,
) {
    { constraints ->
        require(constraints.maxWidth != Constraints.Infinity) {
            "LazyVerticalGrid's width should be bound by parent"
        }
        val horizontalPadding = contentPadding.calculateStartPadding(LayoutDirection.Ltr) +
            contentPadding.calculateEndPadding(LayoutDirection.Ltr)
        val gridWidth = constraints.maxWidth - horizontalPadding.roundToPx()
        with(columns) {
            calculateCrossAxisCellSizes(
                gridWidth,
                horizontalArrangement.spacing.roundToPx(),
            ).toMutableList().apply {
                for (i in 1..<size) {
                    this[i] += this[i - 1]
                }
            }
        }
    }
}



private class MutableData<T>(var value: T)

object Scroller {
    const val STICKY_HEADER_KEY_PREFIX = "sticky:"
    const val EXACT_HEIGHT_KEY_PREFIX = "exact:"
}

private val ThumbLength = 48.dp
private val ThumbThickness = 12.dp
private val ThumbShape = RoundedCornerShape(ThumbThickness / 2)
private val ScrollBarVisibilityDurationMillis = 2000L
private val ImmediateFadeOutAnimationSpec = tween<Float>(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
)
private val LazyListItemInfo.top: Int
    get() = offset

private val LazyListItemInfo.bottom: Int
    get() = offset + size
