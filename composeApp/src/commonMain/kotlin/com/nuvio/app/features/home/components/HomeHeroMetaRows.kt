package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.i18n.localizedMediaStatusLabel
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.details.components.DetailHeroMetaBadge
import com.nuvio.app.features.details.components.ImdbRatingSourceLabel
import com.nuvio.app.features.details.components.ImdbYellow
import com.nuvio.app.features.details.components.RatingsRow
import com.nuvio.app.features.details.components.formatOneDecimal
import com.nuvio.app.features.details.formatRuntimeForDisplay
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.media_movie
import nuvio.composeapp.generated.resources.media_series
import org.jetbrains.compose.resources.stringResource

private const val HERO_RATINGS_LIMIT = 3
private const val DESKTOP_HERO_GENRES_LIMIT = 3

@Composable
internal fun HomeHeroMetaRows(
    item: MetaPreview,
    isTablet: Boolean,
    showImdbRating: Boolean,
    showMdbListRatings: Boolean,
) {
    val type = item.type.trim().lowercase()
    val isSeries = type == "series" || type == "tv"
    val typeLabel = when (type) {
        "movie" -> stringResource(Res.string.media_movie)
        "series" -> stringResource(Res.string.media_series)
        else -> item.type.replaceFirstChar(Char::uppercase)
    }
    val genres = item.genres
        .asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .take(DESKTOP_HERO_GENRES_LIMIT)
        .toList()
    val runtime = formatRuntimeForDisplay(item.runtime)?.takeIf { it.isNotBlank() }
    val release = item.releaseInfo?.takeIf { it.isNotBlank() }?.let(::formatReleaseDateForDisplay)
    val ageRating = item.ageRating?.trim()?.takeIf { it.isNotBlank() }
    val status = item.status?.trim()?.takeIf { it.isNotBlank() }
        ?.let { localizedMediaStatusLabel(it, isSeries).uppercase() }
    val badges = listOfNotNull(ageRating, status)
    val language = item.language?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
    val ratings = if (showMdbListRatings) item.externalRatings else emptyList()
    val imdbRating = item.imdbRating?.trim()
        ?.takeIf { showImdbRating && ratings.isEmpty() }
        ?.toDoubleOrNull()
        ?.takeIf { it > 0.0 }
        ?.let(::formatOneDecimal)

    if (isDesktop) {
        DesktopHomeHeroMetaPills(
            typeLabel = typeLabel,
            genres = genres,
            runtime = runtime,
            release = release,
            ageRating = ageRating,
            status = status,
            language = language,
            imdbRating = imdbRating,
            externalRatings = ratings,
        )
        return
    }

    val genre = genres.firstOrNull()
    val primaryImdbRating = imdbRating?.takeIf { !isSeries && badges.isEmpty() }
    val secondaryImdbRating = imdbRating?.takeIf { primaryImdbRating == null }
    val horizontalAlignment = if (isTablet) Alignment.Start else Alignment.CenterHorizontally
    val rowArrangement = Arrangement.spacedBy(8.dp, horizontalAlignment)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = rowArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroMetaText(text = typeLabel)
            genre?.let {
                HeroMetaDot()
                HeroMetaText(text = it, modifier = Modifier.weight(1f, fill = false))
            }
            runtime?.let {
                HeroMetaDot()
                HeroMetaText(text = it)
            }
            release?.let {
                HeroMetaDot()
                HeroMetaText(text = it)
            }
            primaryImdbRating?.let {
                HeroMetaDot()
                HeroImdbRating(rating = it)
            }
        }

        if (badges.isNotEmpty() || secondaryImdbRating != null || language != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = rowArrangement,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (badges.isNotEmpty()) {
                    DetailHeroMetaBadge(
                        text = badges.first(),
                        secondaryText = badges.getOrNull(1),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    )
                }
                secondaryImdbRating?.let {
                    if (badges.isNotEmpty()) HeroMetaDot()
                    HeroImdbRating(rating = it)
                }
                language?.let {
                    if (badges.isNotEmpty() || secondaryImdbRating != null) HeroMetaDot()
                    HeroMetaText(text = it)
                }
            }
        }

        if (ratings.isNotEmpty()) {
            RatingsRow(
                ratings = ratings,
                maxItems = HERO_RATINGS_LIMIT,
            )
        }
    }
}

@Composable
private fun DesktopHomeHeroMetaPills(
    typeLabel: String,
    genres: List<String>,
    runtime: String?,
    release: String?,
    ageRating: String?,
    status: String?,
    language: String?,
    imdbRating: String?,
    externalRatings: List<MetaExternalRating>,
) {
    val tokens = MaterialTheme.nuvio
    val ratingTextStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = NuvioTokens.Type.bodySm,
        fontWeight = FontWeight.Bold,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
        ) {
            imdbRating?.let { rating ->
                HeroGlassMetaPill {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = ImdbYellow,
                        modifier = Modifier.size(14.dp),
                    )
                    ImdbRatingSourceLabel(
                        storeTextStyle = ratingTextStyle,
                        storeTextColor = ImdbYellow,
                    )
                    Text(
                        text = rating,
                        style = ratingTextStyle,
                        color = ImdbYellow,
                        maxLines = 1,
                    )
                }
            }

            if (typeLabel.isNotBlank()) {
                HeroGlassMetaPill {
                    HeroGlassMetaText(text = typeLabel, color = tokens.colors.textPrimary)
                }
            }

            release?.let { year ->
                HeroGlassMetaPill {
                    HeroGlassMetaText(text = year, color = tokens.colors.textPrimary)
                }
            }

            runtime?.let { duration ->
                HeroGlassMetaPill {
                    HeroGlassMetaText(text = duration, color = tokens.colors.textPrimary)
                }
            }

            ageRating?.let { ratingBadge ->
                HeroGlassMetaPill(borderColor = tokens.colors.borderDefault) {
                    HeroGlassMetaText(text = ratingBadge, color = tokens.colors.textPrimary)
                }
            }

            status?.let { statusBadge ->
                HeroGlassMetaPill {
                    HeroGlassMetaText(text = statusBadge, color = tokens.colors.textSecondary)
                }
            }

            language?.let { lang ->
                HeroGlassMetaPill {
                    HeroGlassMetaText(text = lang, color = tokens.colors.textSecondary)
                }
            }

            genres.forEach { genre ->
                HeroGlassMetaPill {
                    HeroGlassMetaText(text = genre, color = tokens.colors.textSecondary)
                }
            }
        }

        if (externalRatings.isNotEmpty()) {
            HeroGlassMetaPill {
                RatingsRow(
                    ratings = externalRatings,
                    maxItems = Int.MAX_VALUE,
                )
            }
        }
    }
}

@Composable
private fun HeroGlassMetaPill(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.nuvio.colors.borderSubtle,
    containerColor: Color = MaterialTheme.nuvio.colors.surfaceElevated.copy(alpha = 0.74f),
    content: @Composable RowScope.() -> Unit,
) {
    val pillShape = RoundedCornerShape(NuvioTokens.Radius.full)
    Row(
        modifier = modifier
            .clip(pillShape)
            .background(containerColor)
            .border(NuvioTokens.Border.thin, borderColor, pillShape)
            .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s5),
        content = content,
    )
}

@Composable
private fun HeroGlassMetaText(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = NuvioTokens.Type.bodySm,
            fontWeight = FontWeight.SemiBold,
        ),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeroImdbRating(rating: String) {
    val textStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    Row(verticalAlignment = Alignment.CenterVertically) {
        ImdbRatingSourceLabel(storeTextStyle = textStyle)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = rating,
            style = textStyle,
            color = ImdbYellow,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeroMetaText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeroMetaDot() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
    )
}
