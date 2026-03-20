package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class PanoramaMode {
    FOLLOW_GLOBAL,
    FORCE_ON,
    FORCE_OFF;

    fun resolve(global: Boolean): Boolean = when (this) {
        FOLLOW_GLOBAL -> global
        FORCE_ON -> true
        FORCE_OFF -> false
    }

    fun getLabelRes(): StringResource = when (this) {
        FOLLOW_GLOBAL -> MR.strings.label_default
        FORCE_ON -> MR.strings.action_on
        FORCE_OFF -> MR.strings.action_off
    }
}
