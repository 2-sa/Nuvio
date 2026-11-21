package com.nuvio.app.features.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.isDesktop
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.nuvio.app.core.i18n.localizedShortMonthName
import com.nuvio.app.core.ui.Chip
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioDesktopVerticalScrollbar
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.desktopPageHorizontalPaddingForWidth
import com.nuvio.app.core.ui.SkeletonPosterRow
import com.nuvio.app.core.ui.landscapePosterHeightForWidth
import com.nuvio.app.core.ui.landscapePosterWidth
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.skeleton
import com.nuvio.app.core.ui.withDuplicateSafeLazyKeys
import com.nuvio.app.features.details.components.DetailPosterRailSection
import com.nuvio.app.features.details.components.ExpandableDescription
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.tmdb.TmdbMetadataService
import com.nuvio.app.core.poster.withCustomPosterUrls
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watching.application.WatchingState
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.navigation.LocalUseNativeNavigation

private sealed interface PersonDetailUiState {
    data object Loading : PersonDetailUiState
    data class Success(val personDetail: PersonDetail) : PersonDetailUiState
    data class Error(val message: String) : PersonDetailUiState
}

private enum class PersonMediaFilter {
    ALL,
    MOVIES,
    SERIES,
}

private enum class PersonCreditSort {
    POPULAR,
    LATEST,
    UPCOMING,
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun PersonDetailScreen(
    personId: Int,
    personName: String,
    initialProfilePhoto: String? = null,
    avatarTransitionKey: String? = null,
    preferCrew: Boolean = false,
    onBack: () -> Unit,
    onOpenMeta: (MetaPreview) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    var retryNonce by remember(personId) { mutableIntStateOf(0) }
    var uiState by remember(personId) { mutableStateOf<PersonDetailUiState>(PersonDetailUiState.Loading) }
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val resolvedAvatarTransitionKey = avatarTransitionKey ?: castAvatarSharedTransitionKey(personId)

    LaunchedEffect(personId, retryNonce) {
        uiState = PersonDetailUiState.Loading
        val detail = TmdbMetadataService.fetchPersonDetail(
            personId = personId,
            preferCrewCredits = preferCrew,
        )
        uiState = if (detail != null) {
            val pattern = com.nuvio.app.core.poster.CustomPosterUrlRepository.let { repo ->
                repo.ensureLoaded()
                repo.patternForScreen(com.nuvio.app.core.poster.CustomPosterScreen.DETAILS)
            }
            PersonDetailUiState.Success(detail.withCustomPosterUrls(pattern))
        } else {
            PersonDetailUiState.Error(getString(Res.string.person_load_failed, personName))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.background),
    ) {
        when (val state = uiState) {
            is PersonDetailUiState.Loading -> PersonDetailSkeleton(
                personId = personId,
                personName = personName,
                profilePhoto = initialProfilePhoto,
                avatarTransitionKey = resolvedAvatarTransitionKey,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
            is PersonDetailUiState.Error -> PersonDetailError(
                message = state.message,
                onRetry = {
                    retryNonce += 1
                },
            )
            is PersonDetailUiState.Success -> PersonDetailContent(
                person = state.personDetail,
                watchedKeys = watchedUiState.watchedKeys,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                onOpenMeta = onOpenMeta,
                initialProfilePhoto = initialProfilePhoto,
                avatarTransitionKey = resolvedAvatarTransitionKey,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }

        if (!LocalUseNativeNavigation.current) {
            if (isDesktop) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    NuvioBackButton(
                        onClick = onBack,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(
                                start = desktopPageHorizontalPaddingForWidth(maxWidth.value),
                                top = NuvioTokens.Space.s24,
                            )
                            .align(Alignment.TopStart),
                        containerColor = tokens.colors.surfaceElevated.copy(alpha = 0.78f),
                        showContainerOnDesktop = true,
                        contentColor = tokens.colors.textPrimary,
                        buttonSize = 40.dp,
                        iconSize = 20.dp,
                    )
                }
            } else {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 4.dp, top = 4.dp)
                        .align(Alignment.TopStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(Res.string.action_back),
                        tint = tokens.colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun PersonDetailContent(
    person: PersonDetail,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onOpenMeta: (MetaPreview) -> Unit,
    initialProfilePhoto: String? = null,
    avatarTransitionKey: String,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val tokens = MaterialTheme.nuvio
    val posterCardStyle = rememberPosterCardStyleUiState()
    val accentColor = tokens.colors.accent

    val allCredits = remember(person.movieCredits, person.tvCredits) {
        (person.movieCredits + person.tvCredits)
            .distinctBy { it.id }
    }

    val todayDate = remember { CurrentDateProvider.todayIsoDate() }

    val popularCredits = remember(allCredits) {
        allCredits
            .sortedByDescending { it.popularity ?: 0.0 }
    }

    val latestCredits = remember(allCredits, todayDate) {
        allCredits
            .filter { credit ->
                val date = credit.rawReleaseDate
                date != null && date <= todayDate
            }
            .sortedByDescending { it.rawReleaseDate ?: "" }
    }

    val upcomingCredits = remember(allCredits, todayDate) {
        allCredits
            .filter { credit ->
                val date = credit.rawReleaseDate
                date != null && date > todayDate
            }
            .sortedBy { it.rawReleaseDate ?: "" }
    }

    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    // Hero collapse: 0 = fully expanded, 1 = fully collapsed
    val collapseProgress by remember {
        derivedStateOf {
            (scrollState.value / HERO_COLLAPSE_SCROLL_RANGE).coerceIn(0f, 1f)
        }
    }

    val shouldTriggerCatalogHaptic by remember {
        derivedStateOf { scrollState.value >= HAPTIC_TRIGGER_SCROLL_THRESHOLD_PX }
    }
    var didTriggerCatalogHaptic by remember(person.tmdbId) { mutableStateOf(false) }
    LaunchedEffect(shouldTriggerCatalogHaptic, didTriggerCatalogHaptic) {
        if (shouldTriggerCatalogHaptic && !didTriggerCatalogHaptic) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            didTriggerCatalogHaptic = true
        }
    }

    val accentGradient = remember(accentColor) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to accentColor.copy(alpha = 0.14f),
                0.18f to accentColor.copy(alpha = 0.06f),
                0.38f to Color.Transparent,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(accentGradient),
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useDesktopSplitLayout = isDesktop && maxWidth >= 720.dp
            val useWideLayout = !isDesktop && maxWidth >= PERSON_DETAIL_WIDE_LAYOUT_MIN_WIDTH
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
            ) {
                if (useDesktopSplitLayout) {
                    DesktopPersonDetailContent(
                        person = person,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        todayDate = todayDate,
                        pageHorizontalPadding = desktopPageHorizontalPaddingForWidth(maxWidth.value),
                        onOpenMeta = onOpenMeta,
                        fallbackProfilePhoto = initialProfilePhoto,
                        avatarTransitionKey = avatarTransitionKey,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                } else if (useWideLayout) {
                    WidePersonDetailContent(
                        person = person,
                        popularCredits = popularCredits,
                        latestCredits = latestCredits,
                        upcomingCredits = upcomingCredits,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        onOpenMeta = onOpenMeta,
                        fallbackProfilePhoto = initialProfilePhoto,
                        avatarTransitionKey = avatarTransitionKey,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(top = 48.dp),
                        ) {
                            HeroSection(
                                person = person,
                                collapseProgress = collapseProgress,
                                fallbackProfilePhoto = initialProfilePhoto,
                                avatarTransitionKey = avatarTransitionKey,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                            )

                            if (popularCredits.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                DetailPosterRailSection(
                                    title = stringResource(Res.string.person_popular),
                                    items = popularCredits,
                                    watchedKeys = watchedKeys,
                                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                    headerHorizontalPadding = 20.dp,
                                    onPosterClick = onOpenMeta,
                                )
                            }

                            if (latestCredits.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                DetailPosterRailSection(
                                    title = stringResource(Res.string.person_latest),
                                    items = latestCredits,
                                    watchedKeys = watchedKeys,
                                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                    headerHorizontalPadding = 20.dp,
                                    onPosterClick = onOpenMeta,
                                )
                            }

                            if (upcomingCredits.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                DetailPosterRailSection(
                                    title = stringResource(Res.string.person_upcoming),
                                    items = upcomingCredits,
                                    watchedKeys = watchedKeys,
                                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                    headerHorizontalPadding = 20.dp,
                                    onPosterClick = onOpenMeta,
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        NuvioDesktopVerticalScrollbar(
                            state = scrollState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun DesktopPersonDetailContent(
    person: PersonDetail,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    todayDate: String,
    pageHorizontalPadding: Dp,
    onOpenMeta: (MetaPreview) -> Unit,
    fallbackProfilePhoto: String?,
    avatarTransitionKey: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val tokens = MaterialTheme.nuvio
    val posterCardStyle = rememberPosterCardStyleUiState()
    var selectedMediaFilterName by rememberSaveable(person.tmdbId) {
        mutableStateOf(PersonMediaFilter.ALL.name)
    }
    var selectedSortName by rememberSaveable(person.tmdbId) {
        mutableStateOf(PersonCreditSort.POPULAR.name)
    }

    val selectedMediaFilter = remember(selectedMediaFilterName) {
        PersonMediaFilter.entries.firstOrNull { it.name == selectedMediaFilterName } ?: PersonMediaFilter.ALL
    }
    val selectedSort = remember(selectedSortName) {
        PersonCreditSort.entries.firstOrNull { it.name == selectedSortName } ?: PersonCreditSort.POPULAR
    }

    val baseCredits = remember(person.movieCredits, person.tvCredits, selectedMediaFilter) {
        when (selectedMediaFilter) {
            PersonMediaFilter.ALL -> (person.movieCredits + person.tvCredits).distinctBy { it.id }
            PersonMediaFilter.MOVIES -> person.movieCredits.distinctBy { it.id }
            PersonMediaFilter.SERIES -> person.tvCredits.distinctBy { it.id }
        }
    }
    val hasUpcomingCredits = remember(baseCredits, todayDate) {
        baseCredits.any { credit ->
            val date = credit.rawReleaseDate
            date != null && date > todayDate
        }
    }
    val effectiveSort = if (selectedSort == PersonCreditSort.UPCOMING && !hasUpcomingCredits) {
        PersonCreditSort.POPULAR
    } else {
        selectedSort
    }
    val displayedCredits = remember(baseCredits, effectiveSort, todayDate) {
        when (effectiveSort) {
            PersonCreditSort.POPULAR -> baseCredits.sortedByDescending { it.popularity ?: 0.0 }
            PersonCreditSort.LATEST -> baseCredits
                .filter { credit ->
                    val date = credit.rawReleaseDate
                    date != null && date <= todayDate
                }
                .sortedByDescending { it.rawReleaseDate ?: "" }
            PersonCreditSort.UPCOMING -> baseCredits
                .filter { credit ->
                    val date = credit.rawReleaseDate
                    date != null && date > todayDate
                }
                .sortedBy { it.rawReleaseDate ?: "" }
        }
    }

    val effectiveBaseWidthDp = desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp)
    val gridCellWidth = if (posterCardStyle.catalogLandscapeModeEnabled) {
        landscapePosterWidth(effectiveBaseWidthDp)
    } else {
        effectiveBaseWidthDp.dp
    }
    val gridState = rememberLazyGridState()
    val hasMovies = person.movieCredits.isNotEmpty()
    val hasSeries = person.tvCredits.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.FixedSize(gridCellWidth),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = pageHorizontalPadding,
                top = 80.dp,
                end = pageHorizontalPadding,
                bottom = nuvioSafeBottomPadding(36.dp),
            ),
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s16),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s20),
        ) {
            item(
                key = "person-split-header",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                DesktopPersonSplitHeaderCard(
                    person = person,
                    fallbackProfilePhoto = fallbackProfilePhoto,
                    avatarTransitionKey = avatarTransitionKey,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }

            item(
                key = "person-filter-bar",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (hasMovies && hasSeries) {
                                Chip(
                                    label = stringResource(Res.string.home_filter_all),
                                    selected = selectedMediaFilter == PersonMediaFilter.ALL,
                                    onClick = { selectedMediaFilterName = PersonMediaFilter.ALL.name },
                                )
                                Chip(
                                    label = stringResource(Res.string.home_filter_movies),
                                    selected = selectedMediaFilter == PersonMediaFilter.MOVIES,
                                    onClick = { selectedMediaFilterName = PersonMediaFilter.MOVIES.name },
                                )
                                Chip(
                                    label = stringResource(Res.string.home_filter_series),
                                    selected = selectedMediaFilter == PersonMediaFilter.SERIES,
                                    onClick = { selectedMediaFilterName = PersonMediaFilter.SERIES.name },
                                )
                                Spacer(modifier = Modifier.width(NuvioTokens.Space.s8))
                            }
                            Chip(
                                label = stringResource(Res.string.person_popular),
                                selected = effectiveSort == PersonCreditSort.POPULAR,
                                onClick = { selectedSortName = PersonCreditSort.POPULAR.name },
                            )
                            Chip(
                                label = stringResource(Res.string.person_latest),
                                selected = effectiveSort == PersonCreditSort.LATEST,
                                onClick = { selectedSortName = PersonCreditSort.LATEST.name },
                            )
                            if (hasUpcomingCredits) {
                                Chip(
                                    label = stringResource(Res.string.person_upcoming),
                                    selected = effectiveSort == PersonCreditSort.UPCOMING,
                                    onClick = { selectedSortName = PersonCreditSort.UPCOMING.name },
                                )
                            }
                        }

                        if (displayedCredits.isNotEmpty()) {
                            Text(
                                text = if (displayedCredits.size == 1) {
                                    stringResource(Res.string.entity_browse_title_count_one, displayedCredits.size)
                                } else {
                                    stringResource(Res.string.entity_browse_title_count_other, displayedCredits.size)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.colors.textMuted,
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = NuvioTokens.Border.thin,
                        color = tokens.colors.borderSubtle,
                    )
                }
            }

            if (displayedCredits.isEmpty()) {
                item(
                    key = "person-empty-filmography",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = NuvioTokens.Space.s40),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.catalog_empty_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.colors.textMuted,
                        )
                    }
                }
            } else {
                items(
                    items = displayedCredits.withDuplicateSafeLazyKeys { it.stableKey() },
                    key = { it.lazyKey },
                ) { keyedItem ->
                    val item = keyedItem.value
                    HomePosterCard(
                        item = item,
                        useLandscapeBackdropMode = posterCardStyle.catalogLandscapeModeEnabled,
                        isWatched = WatchingState.isPosterWatched(
                            watchedKeys = watchedKeys,
                            item = item,
                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        ),
                        onClick = { onOpenMeta(item) },
                    )
                }
            }
        }

        NuvioDesktopVerticalScrollbar(
            state = gridState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 8.dp, horizontal = 4.dp),
        )
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun DesktopPersonSplitHeaderCard(
    person: PersonDetail,
    fallbackProfilePhoto: String?,
    avatarTransitionKey: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(NuvioTokens.Radius.xl)
    val portraitShape = RoundedCornerShape(NuvioTokens.Radius.xl)
    val avatarUrl = person.profilePhoto?.takeIf { it.isNotBlank() } ?: fallbackProfilePhoto
    val platformContext = LocalPlatformContext.current
    val avatarRequest = if (!avatarUrl.isNullOrBlank()) {
        remember(platformContext, avatarUrl, avatarTransitionKey) {
            ImageRequest.Builder(platformContext)
                .data(avatarUrl)
                .memoryCacheKey(avatarTransitionKey)
                .placeholderMemoryCacheKey(avatarTransitionKey)
                .diskCacheKey(avatarUrl)
                .build()
        }
    } else {
        null
    }
    val avatarSharedElementModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = avatarTransitionKey),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }
    val credits = remember(person.movieCredits, person.tvCredits) {
        (person.movieCredits + person.tvCredits).distinctBy { it.id }
    }
    val creditSummary = remember(credits) { buildCreditSummary(credits) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceElevated)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        tokens.colors.accent.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                ),
            )
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
            .padding(NuvioTokens.Space.s24),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s24),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .then(avatarSharedElementModifier)
                    .width(164.dp)
                    .height(216.dp)
                    .clip(portraitShape)
                    .background(tokens.colors.surfaceCard)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, portraitShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarRequest ?: avatarUrl,
                        contentDescription = person.name,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = person.name.initials(),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.colors.textSecondary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
            ) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = tokens.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                ) {
                    person.knownFor?.takeIf { it.isNotBlank() }?.let { knownFor ->
                        PersonMetadataPill(
                            text = stringResource(Res.string.person_known_for, knownFor),
                            isAccent = true,
                        )
                    }
                    person.birthday?.takeIf { it.isNotBlank() }?.let { birthday ->
                        PersonMetadataPill(
                            text = personBirthLine(birthday = birthday, deathday = person.deathday),
                        )
                    }
                    person.placeOfBirth?.takeIf { it.isNotBlank() }?.let { place ->
                        PersonMetadataPill(text = place)
                    }
                    if (creditSummary.isNotBlank()) {
                        PersonMetadataPill(text = creditSummary)
                    }
                }

                person.biography?.takeIf { it.isNotBlank() }?.let { biography ->
                    ExpandableDescription(
                        text = biography,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = tokens.colors.textSecondary,
                        collapsedMaxLines = 5,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonMetadataPill(
    text: String,
    isAccent: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(NuvioTokens.Radius.md)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (isAccent) tokens.colors.focusBackground else tokens.colors.surfaceCard)
            .border(
                width = NuvioTokens.Border.thin,
                color = if (isAccent) tokens.colors.accent.copy(alpha = 0.55f) else tokens.colors.borderSubtle,
                shape = shape,
            )
            .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s6),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isAccent) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (isAccent) tokens.colors.textPrimary else tokens.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val PERSON_DETAIL_WIDE_LAYOUT_MIN_WIDTH = 900.dp
private val PERSON_DETAIL_WIDE_SIDEBAR_WIDTH = 392.dp

private const val HERO_COLLAPSE_SCROLL_RANGE = 220f
private const val HAPTIC_TRIGGER_SCROLL_THRESHOLD_PX = 56

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun WidePersonDetailContent(
    person: PersonDetail,
    popularCredits: List<MetaPreview>,
    latestCredits: List<MetaPreview>,
    upcomingCredits: List<MetaPreview>,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onOpenMeta: (MetaPreview) -> Unit,
    fallbackProfilePhoto: String?,
    avatarTransitionKey: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 34.dp),
    ) {
        PersonIdentitySidebar(
            person = person,
            fallbackProfilePhoto = fallbackProfilePhoto,
            avatarTransitionKey = avatarTransitionKey,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier
                .width(PERSON_DETAIL_WIDE_SIDEBAR_WIDTH)
                .fillMaxHeight(),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
        )

        val contentScrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScrollState)
                    .padding(start = 40.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(34.dp),
            ) {
                if (popularCredits.isNotEmpty()) {
                    DetailPosterRailSection(
                        title = stringResource(Res.string.person_popular),
                        items = popularCredits,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        headerHorizontalPadding = 0.dp,
                        onPosterClick = onOpenMeta,
                    )
                }

                if (latestCredits.isNotEmpty()) {
                    DetailPosterRailSection(
                        title = stringResource(Res.string.person_latest),
                        items = latestCredits,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        headerHorizontalPadding = 0.dp,
                        onPosterClick = onOpenMeta,
                    )
                }

                if (upcomingCredits.isNotEmpty()) {
                    DetailPosterRailSection(
                        title = stringResource(Res.string.person_upcoming),
                        items = upcomingCredits,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        headerHorizontalPadding = 0.dp,
                        onPosterClick = onOpenMeta,
                    )
                }
            }
            NuvioDesktopVerticalScrollbar(
                state = contentScrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun PersonIdentitySidebar(
    person: PersonDetail,
    fallbackProfilePhoto: String?,
    avatarTransitionKey: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val avatarUrl = person.profilePhoto?.takeIf { it.isNotBlank() } ?: fallbackProfilePhoto
    val platformContext = LocalPlatformContext.current
    val avatarRequest = if (!avatarUrl.isNullOrBlank()) {
        remember(platformContext, avatarUrl, avatarTransitionKey) {
            ImageRequest.Builder(platformContext)
                .data(avatarUrl)
                .memoryCacheKey(avatarTransitionKey)
                .placeholderMemoryCacheKey(avatarTransitionKey)
                .diskCacheKey(avatarUrl)
                .build()
        }
    } else {
        null
    }
    val avatarSharedElementModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = avatarTransitionKey),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }
    val credits = remember(person.movieCredits, person.tvCredits) {
        (person.movieCredits + person.tvCredits).distinctBy { it.id }
    }
    val creditSummary = remember(credits) {
        buildCreditSummary(credits)
    }
    val scrollState = rememberScrollState()

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = 40.dp,
                    end = 36.dp,
                    top = if (isDesktop) 72.dp else 40.dp,
                    bottom = 42.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Box(
                modifier = Modifier.size(162.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                )
                Box(
                    modifier = Modifier
                        .then(avatarSharedElementModifier)
                        .size(148.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarRequest ?: avatarUrl,
                            contentDescription = person.name,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = person.name.initials(),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 34.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                person.birthday?.let { birthday ->
                    PersonSidebarFact(
                        label = stringResource(Res.string.person_detail_born),
                        value = personBirthLine(birthday = birthday, deathday = person.deathday),
                    )
                }
                person.placeOfBirth?.takeIf { it.isNotBlank() }?.let { place ->
                    PersonSidebarFact(label = stringResource(Res.string.person_detail_place_of_birth), value = place)
                }
                if (creditSummary.isNotBlank()) {
                    PersonSidebarFact(label = stringResource(Res.string.person_detail_credits), value = creditSummary)
                }
            }

            person.biography?.takeIf { it.isNotBlank() }?.let { biography ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SidebarLabel(text = stringResource(Res.string.person_detail_biography))
                    ExpandableDescription(
                        text = biography,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        collapsedMaxLines = 12,
                    )
                }
            }
        }
        NuvioDesktopVerticalScrollbar(
            state = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 8.dp, horizontal = 4.dp),
        )
    }
}

@Composable
private fun PersonSidebarFact(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        SidebarLabel(text = label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SidebarLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun HeroSection(
    person: PersonDetail,
    collapseProgress: Float = 0f,
    fallbackProfilePhoto: String? = null,
    avatarTransitionKey: String,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val avatarSize = lerp(140.dp, 72.dp, collapseProgress)
    val heroScale = 1f - (collapseProgress * 0.12f)
    val heroAlpha = 1f - (collapseProgress * 0.35f)
    val avatarUrl = person.profilePhoto?.takeIf { it.isNotBlank() } ?: fallbackProfilePhoto
    val avatarCacheKey = avatarTransitionKey
    val platformContext = LocalPlatformContext.current
    val avatarRequest = if (!avatarUrl.isNullOrBlank()) {
        remember(platformContext, avatarUrl, avatarCacheKey) {
            ImageRequest.Builder(platformContext)
                .data(avatarUrl)
                .memoryCacheKey(avatarCacheKey)
                .placeholderMemoryCacheKey(avatarCacheKey)
                .diskCacheKey(avatarUrl)
                .build()
        }
    } else {
        null
    }
    val avatarSharedElementModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(
                    key = avatarTransitionKey,
                ),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .graphicsLayer {
                scaleX = heroScale
                scaleY = heroScale
                alpha = heroAlpha
                translationY = -(collapseProgress * 40f)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Profile Photo
        Box(
            modifier = Modifier
                .then(avatarSharedElementModifier)
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarRequest ?: avatarUrl,
                    contentDescription = person.name,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = person.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text(
            text = person.name,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Personal info
        val infoItems = buildList {
            person.birthday?.let { bday ->
                val age = calculateAge(bday, person.deathday)
                val ageStr = if (age != null) stringResource(Res.string.person_age, age) else ""
                val bdayDisplay = formatDateForDisplay(bday) ?: bday
                val deathDisplay = person.deathday?.let { formatDateForDisplay(it) ?: it }
                val line = if (deathDisplay != null) {
                    buildString {
                        append(stringResource(Res.string.person_born, bdayDisplay, ""))
                        append(" — ")
                        append(stringResource(Res.string.person_died, deathDisplay))
                        append(ageStr)
                    }
                } else {
                    stringResource(Res.string.person_born, bdayDisplay, ageStr)
                }
                add(line)
            }
            person.placeOfBirth?.let { add(it) }
            person.knownFor?.let { add(stringResource(Res.string.person_known_for, it)) }
        }
        if (infoItems.isNotEmpty()) {
            infoItems.forEach { info ->
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        // Biography
        person.biography?.let { bio ->
            Spacer(modifier = Modifier.height(12.dp))
            ExpandableDescription(
                text = bio,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                collapsedMaxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─── Loading / Error States ───

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun PersonDetailSkeleton(
    personId: Int,
    personName: String,
    profilePhoto: String? = null,
    avatarTransitionKey: String,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val isLandscapeShelfMode = posterCardStyle.catalogLandscapeModeEnabled
    val skeletonPosterWidth = if (isLandscapeShelfMode) {
        landscapePosterWidth(posterCardStyle.widthDp)
    } else {
        posterCardStyle.widthDp.dp
    }
    val skeletonPosterHeight = if (isLandscapeShelfMode) {
        landscapePosterHeightForWidth(skeletonPosterWidth)
    } else {
        posterCardStyle.heightDp.dp
    }
    val accentColor = MaterialTheme.colorScheme.primary
    val avatarCacheKey = avatarTransitionKey
    val platformContext = LocalPlatformContext.current
    val avatarRequest = if (!profilePhoto.isNullOrBlank()) {
        remember(platformContext, profilePhoto, avatarCacheKey) {
            ImageRequest.Builder(platformContext)
                .data(profilePhoto)
                .memoryCacheKey(avatarCacheKey)
                .placeholderMemoryCacheKey(avatarCacheKey)
                .diskCacheKey(profilePhoto)
                .build()
        }
    } else {
        null
    }
    val accentGradient = remember(accentColor) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to accentColor.copy(alpha = 0.18f),
                0.15f to accentColor.copy(alpha = 0.10f),
                0.30f to accentColor.copy(alpha = 0.04f),
                0.50f to Color.Transparent,
            ),
        )
    }
    val avatarSharedElementModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(
                    key = avatarTransitionKey,
                ),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(accentGradient),
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (isDesktop && maxWidth >= 720.dp) {
                DesktopPersonDetailSkeleton(
                    personName = personName,
                    profilePhoto = profilePhoto,
                    avatarRequest = avatarRequest,
                    avatarSharedElementModifier = avatarSharedElementModifier,
                    pageHorizontalPadding = desktopPageHorizontalPaddingForWidth(maxWidth.value),
                    skeletonPosterWidth = skeletonPosterWidth,
                    skeletonPosterHeight = skeletonPosterHeight,
                    skeletonPosterCornerRadius = posterCardStyle.cornerRadiusDp.dp,
                )
            } else if (maxWidth >= PERSON_DETAIL_WIDE_LAYOUT_MIN_WIDTH) {
                WidePersonDetailSkeleton(
                    personName = personName,
                    profilePhoto = profilePhoto,
                    avatarRequest = avatarRequest,
                    avatarSharedElementModifier = avatarSharedElementModifier,
                    skeletonPosterWidth = skeletonPosterWidth,
                    skeletonPosterHeight = skeletonPosterHeight,
                    skeletonPosterCornerRadius = posterCardStyle.cornerRadiusDp.dp,
                    showPosterLabels = !isLandscapeShelfMode && !posterCardStyle.hideLabelsEnabled,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .then(avatarSharedElementModifier)
                                .size(140.dp)
                                .skeleton(CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!profilePhoto.isNullOrBlank()) {
                                AsyncImage(
                                    model = avatarRequest ?: profilePhoto,
                                    contentDescription = personName,
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    text = personName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = personName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SkeletonLine(
                            widthFraction = 0.58f,
                            height = 14.dp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonLine(
                            widthFraction = 0.42f,
                            height = 14.dp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonLine(
                            widthFraction = 0.34f,
                            height = 14.dp,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        listOf(1.0f, 0.96f, 0.92f, 0.98f, 0.88f, 0.94f, 0.82f, 0.74f).forEachIndexed { index, widthFraction ->
                            SkeletonLine(
                                widthFraction = widthFraction,
                                height = 16.dp,
                            )
                            if (index != 7) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(18.dp)
                                .skeleton(RoundedCornerShape(4.dp)),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SkeletonPosterRow(
                        width = skeletonPosterWidth,
                        height = skeletonPosterHeight,
                        cornerRadius = posterCardStyle.cornerRadiusDp.dp,
                        horizontalPadding = 20.dp,
                        showLabels = !isLandscapeShelfMode && !posterCardStyle.hideLabelsEnabled,
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun DesktopPersonDetailSkeleton(
    personName: String,
    profilePhoto: String?,
    avatarRequest: ImageRequest?,
    avatarSharedElementModifier: Modifier,
    pageHorizontalPadding: Dp,
    skeletonPosterWidth: Dp,
    skeletonPosterHeight: Dp,
    skeletonPosterCornerRadius: Dp,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(NuvioTokens.Radius.xl)
    val portraitShape = RoundedCornerShape(NuvioTokens.Radius.xl)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = pageHorizontalPadding,
                end = pageHorizontalPadding,
                top = 80.dp,
                bottom = 36.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s20),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(tokens.colors.surfaceElevated)
                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
                .padding(NuvioTokens.Space.s24),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s24),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .then(avatarSharedElementModifier)
                        .width(164.dp)
                        .height(216.dp)
                        .clip(portraitShape)
                        .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, portraitShape)
                        .skeleton(portraitShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!profilePhoto.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarRequest ?: profilePhoto,
                            contentDescription = personName,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = personName.initials(),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = tokens.colors.textSecondary,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
                ) {
                    Text(
                        text = personName,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = tokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(26.dp)
                                    .skeleton(RoundedCornerShape(NuvioTokens.Radius.md)),
                            )
                        }
                    }
                    SkeletonLine(widthFraction = 0.92f, height = 14.dp)
                    SkeletonLine(widthFraction = 0.86f, height = 14.dp)
                    SkeletonLine(widthFraction = 0.72f, height = 14.dp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8)) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(32.dp)
                        .skeleton(RoundedCornerShape(NuvioTokens.Radius.md)),
                )
            }
        }

        SkeletonPosterRow(
            width = skeletonPosterWidth,
            height = skeletonPosterHeight,
            cornerRadius = skeletonPosterCornerRadius,
            horizontalPadding = 0.dp,
            showLabels = true,
        )
    }
}

@Composable
private fun WidePersonDetailSkeleton(
    personName: String,
    profilePhoto: String?,
    avatarRequest: ImageRequest?,
    avatarSharedElementModifier: Modifier,
    skeletonPosterWidth: Dp,
    skeletonPosterHeight: Dp,
    skeletonPosterCornerRadius: Dp,
    showPosterLabels: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 34.dp),
    ) {
        Column(
            modifier = Modifier
                .width(PERSON_DETAIL_WIDE_SIDEBAR_WIDTH)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 40.dp, end = 36.dp, top = 40.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Box(
                modifier = Modifier.size(162.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                )
                Box(
                    modifier = Modifier
                        .then(avatarSharedElementModifier)
                        .size(148.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), CircleShape)
                        .skeleton(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!profilePhoto.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarRequest ?: profilePhoto,
                            contentDescription = personName,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = personName.initials(),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                text = personName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 34.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                repeat(3) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SkeletonLine(widthFraction = 0.34f, height = 10.dp)
                        SkeletonLine(widthFraction = if (it == 1) 0.88f else 0.58f, height = 14.dp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonLine(
                    widthFraction = 0.32f,
                    height = 10.dp,
                )
                listOf(0.96f, 1f, 0.92f, 0.98f, 0.84f, 0.90f).forEach { widthFraction ->
                    SkeletonLine(widthFraction = widthFraction, height = 16.dp)
                }
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 40.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            repeat(3) {
                WideSkeletonPosterRail(
                    skeletonPosterWidth = skeletonPosterWidth,
                    skeletonPosterHeight = skeletonPosterHeight,
                    skeletonPosterCornerRadius = skeletonPosterCornerRadius,
                    showPosterLabels = showPosterLabels,
                )
            }
        }
    }
}

@Composable
private fun WideSkeletonPosterRail(
    skeletonPosterWidth: Dp,
    skeletonPosterHeight: Dp,
    skeletonPosterCornerRadius: Dp,
    showPosterLabels: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(18.dp)
                .skeleton(RoundedCornerShape(4.dp)),
        )

        SkeletonPosterRow(
            width = skeletonPosterWidth,
            height = skeletonPosterHeight,
            cornerRadius = skeletonPosterCornerRadius,
            showLabels = showPosterLabels,
        )
    }
}

@Composable
private fun SkeletonLine(
    widthFraction: Float,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .skeleton(RoundedCornerShape(4.dp)),
    )
}

@Composable
private fun PersonDetailError(
    message: String,
    onRetry: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.person_something_wrong),
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.colors.accent,
                    contentColor = tokens.colors.onAccent,
                ),
            ) {
                Text(stringResource(Res.string.action_retry))
            }
        }
    }
}

// ─── Utility ───

private fun String.initials(): String {
    val parts = trim()
        .split(" ")
        .filter { it.isNotBlank() }
    return parts
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifBlank { firstOrNull()?.uppercase() ?: "?" }
}

private fun buildCreditSummary(credits: List<MetaPreview>): String {
    if (credits.isEmpty()) return ""
    val firstYear = credits
        .mapNotNull { it.rawReleaseDate?.take(4)?.toIntOrNull() }
        .minOrNull()
    return buildString {
        append(credits.size)
        append(if (credits.size == 1) " title" else " titles")
        if (firstYear != null) {
            append(" · since ")
            append(firstYear)
        }
    }
}

private fun personBirthLine(birthday: String, deathday: String?): String {
    val birthdayDisplay = formatDateForDisplay(birthday) ?: birthday
    val deathDisplay = deathday?.let { formatDateForDisplay(it) ?: it }
    val age = calculateAge(birthday, deathday)
    return buildString {
        append(birthdayDisplay)
        if (deathDisplay != null) {
            append(" · died ")
            append(deathDisplay)
        }
        if (age != null) {
            append(" · ")
            append(age)
            append(" years")
        }
    }
}

private fun calculateAge(birthday: String, deathday: String?): Int? {
    val birthParts = birthday.split("-").mapNotNull { it.toIntOrNull() }
    if (birthParts.size < 3) return null
    val birthYear = birthParts[0]
    val birthMonth = birthParts[1]
    val birthDay = birthParts[2]

    val endParts = deathday?.split("-")?.mapNotNull { it.toIntOrNull() }
    // Use a rough current date approximation for KMP compatibility
    val endYear: Int
    val endMonth: Int
    val endDay: Int
    if (endParts != null && endParts.size >= 3) {
        endYear = endParts[0]
        endMonth = endParts[1]
        endDay = endParts[2]
    } else {
        // Approximate current date — this is good enough for age display
        endYear = 2026
        endMonth = 4
        endDay = 3
    }

    var age = endYear - birthYear
    if (endMonth < birthMonth || (endMonth == birthMonth && endDay < birthDay)) {
        age--
    }
    return age.takeIf { it >= 0 }
}

private fun formatDateForDisplay(date: String): String? {
    val parts = date.split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size < 3) return null
    val month = parts[1]
    val day = parts[2]
    val year = parts[0]
    return if (month in 1..12) {
        "${localizedShortMonthName(month)} $day, $year"
    } else {
        null
    }
}
