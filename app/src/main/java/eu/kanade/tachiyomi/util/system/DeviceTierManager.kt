package eu.kanade.tachiyomi.util.system

import android.app.ActivityManager
import android.content.Context

object DeviceTierManager {
    enum class Tier { LOW, MID, HIGH }

    fun getTier(context: Context): Tier {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        
        val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val isLowRam = actManager.isLowRamDevice
        
        return when {
            isLowRam -> Tier.LOW
            totalRamGb <= 3.5 -> Tier.LOW
            totalRamGb <= 7.5 -> Tier.MID
            else -> Tier.HIGH
        }
    }
}
