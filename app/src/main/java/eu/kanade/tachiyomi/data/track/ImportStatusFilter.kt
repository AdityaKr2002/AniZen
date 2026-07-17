package eu.kanade.tachiyomi.data.track

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class ImportStatusFilter(val titleRes: StringResource) {
    WATCHING(MR.strings.watching),
    PLAN_TO_WATCH(MR.strings.plan_to_watch),
    COMPLETED(MR.strings.completed),
    ON_HOLD(MR.strings.on_hold),
}
