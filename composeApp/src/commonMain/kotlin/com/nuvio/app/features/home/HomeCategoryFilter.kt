package com.nuvio.app.features.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_filter_all
import nuvio.composeapp.generated.resources.home_filter_anime
import nuvio.composeapp.generated.resources.home_filter_movies
import nuvio.composeapp.generated.resources.home_filter_series
import org.jetbrains.compose.resources.stringResource

enum class HomeCategoryFilter {
    ALL,
    MOVIES,
    SERIES,
    ANIME,
}

private val AnimeIdTokens = listOf(
    "anime",
    "kitsu",
    "mal:",
    "mal_",
    "anilist",
    "anidb",
)

private val AnimeGenreTokens = listOf(
    "anime",
    "animation",
    "أنمي",
    "رسوم متحركة",
)

private val MovieTypeTokens = setOf(
    "movie",
    "movies",
    "film",
    "films",
    "فيلم",
    "أفلام",
    "افلام",
)

private val SeriesTypeTokens = setOf(
    "series",
    "tv",
    "show",
    "shows",
    "tvshow",
    "tvshows",
    "tv series",
    "tv-series",
    "مسلسل",
    "مسلسلات",
)

private val MovieTitleTokens = setOf(
    "movie",
    "movies",
    "film",
    "films",
    "أفلام",
    "افلام",
)

private val SeriesTitleTokens = setOf(
    "series",
    "shows",
    "tv",
    "مسلسلات",
)

internal fun isMovieType(type: String): Boolean =
    type.trim().lowercase() in MovieTypeTokens

internal fun isSeriesType(type: String): Boolean =
    type.trim().lowercase() in SeriesTypeTokens

private fun tokenizeTitleWords(title: String): Set<String> {
    if (title.isBlank()) return emptySet()
    val words = HashSet<String>()
    var start = -1
    for (index in title.indices) {
        if (title[index].isLetterOrDigit()) {
            if (start < 0) start = index
        } else if (start >= 0) {
            words += title.substring(start, index).lowercase()
            start = -1
        }
    }
    if (start >= 0) {
        words += title.substring(start).lowercase()
    }
    return words
}

private fun String.isClearMovieTitle(): Boolean {
    val words = tokenizeTitleWords(this)
    return words.any { it in MovieTitleTokens } && words.none { it in SeriesTitleTokens }
}

private fun String.isClearSeriesTitle(): Boolean {
    val words = tokenizeTitleWords(this)
    return words.any { it in SeriesTitleTokens } && words.none { it in MovieTitleTokens }
}

internal fun isAnimeSignal(
    type: String,
    catalogId: String = "",
    catalogName: String = "",
    itemId: String = "",
    genres: List<String> = emptyList(),
): Boolean {
    if (type.trim().equals("anime", ignoreCase = true)) return true
    val identifierSignals = listOf(catalogId, catalogName, itemId)
    if (identifierSignals.any { signal ->
            signal.isNotBlank() && AnimeIdTokens.any { token -> signal.contains(token, ignoreCase = true) }
        }
    ) {
        return true
    }
    return genres.any { genre ->
        val normalized = genre.trim()
        normalized.isNotEmpty() && AnimeGenreTokens.any { token -> normalized.contains(token, ignoreCase = true) }
    }
}

private fun HomeCatalogSection.catalogIdentifier(): String = when (val catalogTarget = target) {
    is CatalogTarget.Addon -> catalogTarget.catalogId
    is CatalogTarget.Library -> catalogTarget.sectionType
    is CatalogTarget.Server -> catalogTarget.libraryId.orEmpty()
    is CatalogTarget.CollectionSource -> catalogTarget.sourceKey
}

private fun HomeCatalogSection.isCatalogAnime(): Boolean {
    val genreHints = listOfNotNull((target as? CatalogTarget.Addon)?.genre)
    return isAnimeSignal(
        type = target.contentType,
        catalogId = catalogIdentifier(),
        catalogName = title,
        genres = genreHints,
    )
}

internal fun filterHomeCatalogSections(
    sections: List<HomeCatalogSection>,
    filter: HomeCategoryFilter,
): List<HomeCatalogSection> = when (filter) {
    HomeCategoryFilter.ALL -> sections
    HomeCategoryFilter.MOVIES -> sections.mapNotNull { section ->
        when {
            section.isCatalogAnime() -> null
            isMovieType(section.target.contentType) -> section
            isSeriesType(section.target.contentType) -> null
            section.title.isClearMovieTitle() -> section
            else -> {
                val matchingItems = section.items.filter { item ->
                    isMovieType(item.type) && !isAnimeSignal(
                        type = item.type,
                        itemId = item.id,
                        genres = item.genres,
                    )
                }
                if (matchingItems.isEmpty()) null else section.copy(items = matchingItems)
            }
        }
    }
    HomeCategoryFilter.SERIES -> sections.mapNotNull { section ->
        when {
            section.isCatalogAnime() -> null
            isSeriesType(section.target.contentType) -> section
            isMovieType(section.target.contentType) -> null
            section.title.isClearSeriesTitle() -> section
            else -> {
                val matchingItems = section.items.filter { item ->
                    isSeriesType(item.type) && !isAnimeSignal(
                        type = item.type,
                        itemId = item.id,
                        genres = item.genres,
                    )
                }
                if (matchingItems.isEmpty()) null else section.copy(items = matchingItems)
            }
        }
    }
    HomeCategoryFilter.ANIME -> sections.mapNotNull { section ->
        if (section.isCatalogAnime()) {
            section
        } else {
            val matchingItems = section.items.filter { item ->
                isAnimeSignal(
                    type = item.type,
                    itemId = item.id,
                    genres = item.genres,
                )
            }
            if (matchingItems.isEmpty()) {
                null
            } else {
                section.copy(items = matchingItems)
            }
        }
    }
}

internal fun filterContinueWatchingItems(
    items: List<ContinueWatchingItem>,
    filter: HomeCategoryFilter,
): List<ContinueWatchingItem> = when (filter) {
    HomeCategoryFilter.ALL -> items
    HomeCategoryFilter.MOVIES -> items.filter { item ->
        val effectiveId = item.parentMetaId.ifBlank { item.videoId }
        isMovieType(item.parentMetaType) &&
            !isAnimeSignal(type = item.parentMetaType, itemId = effectiveId)
    }
    HomeCategoryFilter.SERIES -> items.filter { item ->
        val effectiveId = item.parentMetaId.ifBlank { item.videoId }
        isSeriesType(item.parentMetaType) &&
            !isAnimeSignal(type = item.parentMetaType, itemId = effectiveId)
    }
    HomeCategoryFilter.ANIME -> items.filter { item ->
        val effectiveId = item.parentMetaId.ifBlank { item.videoId }
        isAnimeSignal(type = item.parentMetaType, itemId = effectiveId)
    }
}

@Composable
internal fun HomeCategoryFilterBar(
    selectedFilter: HomeCategoryFilter,
    onSelectFilter: (HomeCategoryFilter) -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = NuvioTokens.Space.s4),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeCategoryFilter.entries.forEach { filter ->
            val label = when (filter) {
                HomeCategoryFilter.ALL -> stringResource(Res.string.home_filter_all)
                HomeCategoryFilter.MOVIES -> stringResource(Res.string.home_filter_movies)
                HomeCategoryFilter.SERIES -> stringResource(Res.string.home_filter_series)
                HomeCategoryFilter.ANIME -> stringResource(Res.string.home_filter_anime)
            }
            HomeCategoryFilterPill(
                label = label,
                isSelected = filter == selectedFilter,
                onClick = { onSelectFilter(filter) },
            )
        }
    }
}

@Composable
private fun HomeCategoryFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val pillShape = RoundedCornerShape(NuvioTokens.Radius.full)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> tokens.colors.focusBackground
            isDesktop && isHovered -> tokens.colors.surfaceCard
            else -> tokens.colors.surfaceElevated
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "home_filter_pill_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> tokens.colors.accent
            isDesktop && isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderDefault
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "home_filter_pill_border",
    )
    val contentColor = when {
        isSelected || (isDesktop && isHovered) -> tokens.colors.textPrimary
        else -> tokens.colors.textSecondary
    }

    Box(
        modifier = Modifier
            .nuvioFocusBorder(pillShape)
            .clip(pillShape)
            .background(containerColor)
            .border(width = NuvioTokens.Border.thin, color = borderColor, shape = pillShape)
            .semantics { selected = isSelected }
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = NuvioTokens.Space.s16, vertical = NuvioTokens.Space.s8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
