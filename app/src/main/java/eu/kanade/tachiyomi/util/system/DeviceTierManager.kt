package eu.kanade.tachiyomi.util.system

import android.app.ActivityManager
import android.content.Context
import android.os.Build

object DeviceTierManager {
    enum class Tier { LOW, MID, HIGH }

    fun getTier(context: Context): Tier {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val performanceClass = Build.VERSION.MEDIA_PERFORMANCE_CLASS
            return when {
                performanceClass >= Build.VERSION_CODES.TIRAMISU -> Tier.HIGH // MPC 33+ (Android 13+)
                performanceClass >= Build.VERSION_CODES.S -> Tier.MID         // MPC 31 (Android 12)
                else -> Tier.LOW
            }
        }

        return Tier.LOW
    }
}
