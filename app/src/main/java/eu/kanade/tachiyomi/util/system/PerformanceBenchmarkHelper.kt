package eu.kanade.tachiyomi.util.system

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

/**
 * Helper to perform a 30-second performance benchmark within the app.
 * Collects frame metrics and memory usage data.
 */
object PerformanceBenchmarkHelper {

    private var benchmarkJob: Job? = null
    private val frameDurations = CopyOnWriteArrayList<Long>()
    private var isBenchmarking = false

    private var initialMemory: Long = 0
    private var maxMemory: Long = 0
    private var totalFrames: Int = 0
    private var jankFrames: Int = 0

    private val handler = Handler(Looper.getMainLooper())

    @RequiresApi(Build.VERSION_CODES.N)
    private val frameMetricsListener = Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
        val durationNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
        val durationMs = durationNs / 1_000_000
        frameDurations.add(durationMs)
        totalFrames++
        if (durationMs > 17) { // ~60fps threshold
            jankFrames++
        }
    }

    fun startBenchmark(activity: Activity, scope: CoroutineScope, onFinish: (String) -> Unit) {
        if (isBenchmarking) return
        isBenchmarking = true
        frameDurations.clear()
        totalFrames = 0
        jankFrames = 0
        initialMemory = getUsedMemory()
        maxMemory = initialMemory

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.window.addOnFrameMetricsAvailableListener(frameMetricsListener, handler)
        }

        benchmarkJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 30_000) {
                maxMemory = max(maxMemory, getUsedMemory())
                delay(500)
            }
            stopBenchmark(activity, onFinish)
        }
    }

    private fun stopBenchmark(activity: Activity, onFinish: (String) -> Unit) {
        if (!isBenchmarking) return
        isBenchmarking = false
        benchmarkJob?.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.window.removeOnFrameMetricsAvailableListener(frameMetricsListener)
        }

        val report = generateReport()
        onFinish(report)
    }

    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    private fun generateReport(): String {
        val avgFrameTime = if (frameDurations.isNotEmpty()) frameDurations.average() else 0.0
        val sortedFrames = frameDurations.sorted()
        val p90 = if (sortedFrames.isNotEmpty()) sortedFrames[(sortedFrames.size * 0.9).toInt()] else 0
        val p95 = if (sortedFrames.isNotEmpty()) sortedFrames[(sortedFrames.size * 0.95).toInt()] else 0
        val p99 = if (sortedFrames.isNotEmpty()) sortedFrames[(sortedFrames.size * 0.99).toInt()] else 0
        
        val jankPercentage = if (totalFrames > 0) (jankFrames.toDouble() / totalFrames * 100) else 0.0

        return buildString {
            appendLine("--- AniZen Performance Report (30s) ---")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
            appendLine("Total Frames: $totalFrames")
            appendLine("Avg Frame Time: ${"%.2f".format(avgFrameTime)} ms")
            appendLine("P90: ${p90}ms | P95: ${p95}ms | P99: ${p99}ms")
            appendLine("Jank Frames: $jankFrames (${"%.2f".format(jankPercentage)}%)")
            appendLine("Memory Usage: ${initialMemory}MB (Start) -> ${maxMemory}MB (Peak)")
            appendLine("System: ${Runtime.getRuntime().availableProcessors()} Cores")
            appendLine("---------------------------------------")
        }
    }
}
