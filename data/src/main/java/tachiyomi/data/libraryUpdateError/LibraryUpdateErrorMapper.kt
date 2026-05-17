package tachiyomi.data.libraryUpdateError

import tachiyomi.domain.libraryUpdateError.model.LibraryUpdateError

val libraryUpdateErrorMapper: (Long, Long, Long, Long) -> LibraryUpdateError = { id, animeId, messageId, lastUpdate ->
    LibraryUpdateError(
        id = id,
        animeId = animeId,
        messageId = messageId,
        lastUpdate = lastUpdate,
    )
}
