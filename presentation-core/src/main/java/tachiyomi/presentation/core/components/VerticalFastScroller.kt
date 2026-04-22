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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.util.fastMaxBy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.presentation.core.components.Scroller.STICKY_HEADER_KEY_PREFIX
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws vertical fast scroller to a lazy list
 *
 * Set key with [STICKY_HEADER_KEY_PREFIX] prefix to any sticky header item in the list.
 */
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
            val layoutInfo = listState.layoutInfo
            val showScroller = layoutInfo.visibleItemsInfo.size < layoutInfo.totalItemsCount
            if (!showScroller) return@subcompose

            val thumbTopPadding = with(LocalDensity.current) { topContentPadding.toPx() }
            var thumbOffsetY by remember(thumbTopPadding) { mutableFloatStateOf(thumbTopPadding) }

            // 2025 Height Cache for fluid, non-jumpy scrolling
            val heightCache = remember { mutableMapOf<Int, Int>() }

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
                thumbBottomPadding
            val thumbHeightPx = with(LocalDensity.current) { ThumbLength.toPx() }
            val trackHeightPx = (heightPx - thumbHeightPx).coerceAtLeast(1f)

            // When thumb dragged
            LaunchedEffect(thumbOffsetY) {
                if (layoutInfo.totalItemsCount == 0 || !isThumbDragged) return@LaunchedEffect
                val scrollRatio = ((thumbOffsetY - thumbTopPadding) / trackHeightPx).coerceIn(0f, 1f)
                val scrollItem = (layoutInfo.totalItemsCount - 1) * scrollRatio
                val scrollItemRounded = scrollItem.roundToInt()
                val scrollItemSize = layoutInfo.visibleItemsInfo.find { it.index == scrollItemRounded }?.size ?: 0
                val scrollItemOffset = scrollItemSize * (scrollItem - scrollItemRounded)
                listState.scrollToItem(index = scrollItemRounded, scrollOffset = scrollItemOffset.roundToInt())
                scrolled.tryEmit(Unit)
            }

            // When list scrolled
            LaunchedEffect(listState.firstVisibleItemScrollOffset, listState.firstVisibleItemIndex) {
                if (listState.layoutInfo.totalItemsCount == 0 || isThumbDragged) return@LaunchedEffect
                
                val layoutInfo = listState.layoutInfo
                
                // Update height cache with real measurements
                layoutInfo.visibleItemsInfo.forEach { item ->
                    if ((item.key as? String)?.startsWith(STICKY_HEADER_KEY_PREFIX)?.not() ?: true) {
                        heightCache[item.index] = item.size
                    }
                }

                val totalItems = layoutInfo.totalItemsCount
                val avgHeight = if (heightCache.isNotEmpty()) heightCache.values.average().toFloat() else 100f
                
                // Estimate current scroll position using cache
                val firstVisibleIndex = listState.firstVisibleItemIndex
                val firstVisibleOffset = listState.firstVisibleItemScrollOffset
                
                var currentScrollPx = 0f
                for (i in 0 until firstVisibleIndex) {
                    currentScrollPx += heightCache[i]?.toFloat() ?: avgHeight
                }
                currentScrollPx += firstVisibleOffset

                // Estimate total height
                var estimatedTotalHeight = 0f
                for (i in 0 until totalItems) {
                    estimatedTotalHeight += heightCache[i]?.toFloat() ?: avgHeight
                }
                
                // Add content padding to estimated total height
                val totalScrollableHeight = estimatedTotalHeight - layoutInfo.viewportSize.height + layoutInfo.afterContentPadding + layoutInfo.beforeContentPadding

                val proportion = (currentScrollPx / totalScrollableHeight.coerceAtLeast(1f)).coerceIn(0f, 1f)
                
                thumbOffsetY = (trackHeightPx * proportion + thumbTopPadding).coerceIn(
                    thumbTopPadding,
                    thumbTopPadding + trackHeightPx,
                )
                scrolled.tryEmit(Unit)
            }

            // Thumb alpha
            val alpha = remember { Animatable(0f) }
            val isThumbVisible = alpha.value > 0f
            LaunchedEffect(scrolled, alpha) {
                scrolled.collectLatest {
                    if (thumbAllowed()) {
                        alpha.snapTo(1f)
                        delay(2000)
                        alpha.animateTo(0f, animationSpec = tween(durationMillis = 500))
                    } else {
                        alpha.snapTo(0f)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffsetY.roundToInt()) }
                    .then(
                        // Recompose opts
                        if (isThumbVisible && !listState.isScrollInProgress) {
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
                        // Exclude thumb from gesture area only when needed
                        if (isThumbVisible && !isThumbDragged && !listState.isScrollInProgress) {
                            Modifier.systemGestureExclusion()
                        } else {
                            Modifier
                        },
                    )
                    .height(ThumbLength)
                    .padding(horizontal = 8.dp)
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
            val showScroller = layoutInfo.visibleItemsInfo.size < layoutInfo.totalItemsCount
            if (!showScroller) return@subcompose
            val thumbTopPadding = with(LocalDensity.current) { topContentPadding.toPx() }
            var thumbOffsetY by remember(thumbTopPadding) { mutableFloatStateOf(thumbTopPadding) }

            // 2025 Row Height Cache for grids
            val rowHeightCache = remember { mutableMapOf<Int, Int>() }

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
                thumbBottomPadding
            val thumbHeightPx = with(LocalDensity.current) { ThumbLength.toPx() }
            val trackHeightPx = (heightPx - thumbHeightPx).coerceAtLeast(1f)

            val columnCount = remember { slotSizesSums(constraints).size }

            // When thumb dragged
            LaunchedEffect(thumbOffsetY) {
                if (layoutInfo.totalItemsCount == 0 || !isThumbDragged) return@LaunchedEffect
                val scrollRatio = ((thumbOffsetY - thumbTopPadding) / trackHeightPx).coerceIn(0f, 1f)
                val scrollItem = (layoutInfo.totalItemsCount - 1) * scrollRatio
                // I can't think of anything else rn but this'll do
                val scrollItemWhole = scrollItem.toInt()
                val columnNum = ((scrollItemWhole + 1) % columnCount).takeIf { it != 0 } ?: columnCount
                val scrollItemFraction = if (scrollItemWhole == 0) scrollItem else scrollItem % scrollItemWhole
                val offsetPerItem = 1f / columnCount
                val offsetRatio = (offsetPerItem * scrollItemFraction) + (offsetPerItem * (columnNum - 1))

                // TODO: Sometimes item height is not available when scrolling up
                val scrollItemSize = (1..columnCount).maxOf { num ->
                    val actualIndex = if (num != columnNum) {
                        scrollItemWhole + num - columnCount
                    } else {
                        scrollItemWhole
                    }
                    layoutInfo.visibleItemsInfo.find { it.index == actualIndex }?.size?.height ?: 0
                }
                val scrollItemOffset = scrollItemSize * offsetRatio

                state.scrollToItem(index = scrollItemWhole, scrollOffset = scrollItemOffset.roundToInt())
                scrolled.tryEmit(Unit)
            }

            // When list scrolled
            LaunchedEffect(state.firstVisibleItemScrollOffset, state.firstVisibleItemIndex) {
                if (state.layoutInfo.totalItemsCount == 0 || isThumbDragged) return@LaunchedEffect
                
                val layoutInfo = state.layoutInfo
                
                // Update row height cache
                layoutInfo.visibleItemsInfo.forEach { item ->
                    val row = item.index / columnCount
                    rowHeightCache[row] = item.size.height
                }

                val totalItems = layoutInfo.totalItemsCount
                val totalRows = (totalItems + columnCount - 1) / columnCount
                val avgRowHeight = if (rowHeightCache.isNotEmpty()) rowHeightCache.values.average().toFloat() else 200f
                
                val firstVisibleIndex = state.firstVisibleItemIndex
                val firstVisibleRow = firstVisibleIndex / columnCount
                val firstVisibleOffset = state.firstVisibleItemScrollOffset
                
                var currentScrollPx = 0f
                for (i in 0 until firstVisibleRow) {
                    currentScrollPx += rowHeightCache[i]?.toFloat() ?: avgRowHeight
                }
                currentScrollPx += firstVisibleOffset

                var estimatedTotalHeight = 0f
                for (i in 0 until totalRows) {
                    estimatedTotalHeight += rowHeightCache[i]?.toFloat() ?: avgRowHeight
                }

                // Add content padding to estimated total height
                val totalScrollableHeight = estimatedTotalHeight - layoutInfo.viewportSize.height + layoutInfo.afterContentPadding + layoutInfo.beforeContentPadding

                val proportion = (currentScrollPx / totalScrollableHeight.coerceAtLeast(1f)).coerceIn(0f, 1f)
                
                thumbOffsetY = (trackHeightPx * proportion + thumbTopPadding).coerceIn(
                    thumbTopPadding,
                    thumbTopPadding + trackHeightPx,
                )
                scrolled.tryEmit(Unit)
            }

            // Thumb alpha
            val alpha = remember { Animatable(0f) }
            val isThumbVisible = alpha.value > 0f
            LaunchedEffect(scrolled, alpha) {
                scrolled.collectLatest {
                    if (thumbAllowed()) {
                        alpha.snapTo(1f)
                        delay(2000)
                        alpha.animateTo(0f, animationSpec = tween(durationMillis = 500))
                    } else {
                        alpha.snapTo(0f)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffsetY.roundToInt()) }
                    .then(
                        // Recompose opts
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
                        // Exclude thumb from gesture area only when needed
                        if (isThumbVisible && !isThumbDragged && !state.isScrollInProgress) {
                            Modifier.systemGestureExclusion()
                        } else {
                            Modifier
                        },
                    )
                    .height(ThumbLength)
                    .padding(horizontal = 8.dp)
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

object Scroller {
    const val STICKY_HEADER_KEY_PREFIX = "sticky:"
}

private val ThumbLength = 48.dp
private val ThumbThickness = 12.dp
private val ThumbShape = RoundedCornerShape(ThumbThickness / 2)

private val LazyListItemInfo.top: Int
    get() = offset

private val LazyListItemInfo.bottom: Int
    get() = offset + size
