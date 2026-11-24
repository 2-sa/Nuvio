package com.nuvio.app.features.addons

internal enum class AddonCapabilityKind {
    STREAM,
    CATALOGS,
    META,
    SUBTITLES,
}

internal enum class AddonContentTypeKind {
    MOVIE,
    SERIES,
    ANIME,
    CHANNEL,
    OTHER,
}

internal data class AddonCapabilityBadge(
    val kind: AddonCapabilityKind,
    val count: Int? = null,
)

private val IGNORED_ADDON_CONTENT_TYPES = setOf(
    "trakt",
    "collection",
    "collections",
    "person",
    "people",
    "subtitles",
    "subtitle",
)

internal fun extractAddonCapabilityBadges(
    resources: List<String>,
    catalogCount: Int,
): List<AddonCapabilityBadge> {
    val normalizedResources = resources
        .asSequence()
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()

    return buildList {
        if ("stream" in normalizedResources) {
            add(AddonCapabilityBadge(AddonCapabilityKind.STREAM))
        }
        if (catalogCount > 0) {
            add(AddonCapabilityBadge(AddonCapabilityKind.CATALOGS, count = catalogCount))
        }
        if ("meta" in normalizedResources) {
            add(AddonCapabilityBadge(AddonCapabilityKind.META))
        }
        if ("subtitles" in normalizedResources) {
            add(AddonCapabilityBadge(AddonCapabilityKind.SUBTITLES))
        }
    }
}

internal fun normalizeAddonContentTypes(rawTypes: List<String>): List<AddonContentTypeKind> {
    val primary = linkedSetOf<AddonContentTypeKind>()
    var sawOtherOrUnknown = false

    for (raw in rawTypes) {
        val token = raw.trim().lowercase()
        if (token.isEmpty()) continue
        when {
            token == "movie" -> primary.add(AddonContentTypeKind.MOVIE)
            token == "series" -> primary.add(AddonContentTypeKind.SERIES)
            token == "anime" || token.startsWith("anime.") -> primary.add(AddonContentTypeKind.ANIME)
            token == "tv" || token == "channel" || token == "channels" -> primary.add(AddonContentTypeKind.CHANNEL)
            token == "other" -> sawOtherOrUnknown = true
            token in IGNORED_ADDON_CONTENT_TYPES -> Unit
            else -> sawOtherOrUnknown = true
        }
    }

    return when {
        primary.isNotEmpty() -> primary.toList()
        sawOtherOrUnknown -> listOf(AddonContentTypeKind.OTHER)
        else -> emptyList()
    }
}

internal fun isPrimarilyLatinText(text: String): Boolean {
    var latinCount = 0
    var nonLatinLetterCount = 0
    for (ch in text) {
        if (ch in 'A'..'Z' || ch in 'a'..'z') {
            latinCount++
        } else if (ch.isLetter()) {
            nonLatinLetterCount++
        }
    }
    return latinCount > nonLatinLetterCount
}
