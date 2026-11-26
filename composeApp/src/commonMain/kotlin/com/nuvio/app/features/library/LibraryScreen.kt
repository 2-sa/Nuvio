package com.nuvio.app.features.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.i18n.localizedByteUnit
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.poster.CustomPosterScreen
import com.nuvio.app.core.poster.CustomPosterUrlRepository
import com.nuvio.app.core.poster.withCustomPosterUrl
import com.nuvio.app.core.ui.DisintegratingContainer
import com.nuvio.app.core.ui.DisintegrationRequest
import com.nuvio.app.core.ui.FullscreenActionButton
import com.nuvio.app.core.ui.LocalVerticalBarPadding
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.NuvioViewAllPillSize
import com.nuvio.app.core.ui.ScopedDisintegrationTracker
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.core.ui.SkeletonBlock
import com.nuvio.app.core.ui.desktopPageHorizontalPaddingForWidth
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioConsumePointerEvents
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.posterGridColumnCountForViewport
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.shimmer
import com.nuvio.app.features.cloud.CloudLibraryFile
import com.nuvio.app.features.cloud.CloudLibraryItem
import com.nuvio.app.features.cloud.CloudLibraryItemType
import com.nuvio.app.features.cloud.CloudLibraryRepository
import com.nuvio.app.features.cloud.CloudLibraryUiState
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.home.components.HomeSkeletonRow
import com.nuvio.app.features.home.components.PosterGridRow
import com.nuvio.app.features.home.components.PosterGridSkeletonRow
import com.nuvio.app.features.home.components.posterGridColumnCountForWidth
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.servers.ServerRepository
import com.nuvio.app.features.tracking.TrackingRefreshIntent
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watching.application.WatchingState
import com.nuvio.app.isDesktop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    topChromePadding: Dp? = null,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onPosterClick: ((LibraryItem) -> Unit)? = null,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)? = null,
    onSectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)? = null,
    onCloudFilePlay: ((CloudLibraryItem, CloudLibraryFile) -> Unit)? = null,
    onConnectCloudClick: (() -> Unit)? = null,
    onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    onPreviewClick: ((MetaPreview) -> Unit)? = null,
    onPreviewLongClick: ((MetaPreview) -> Unit)? = null,
    onDownloadsClick: (() -> Unit)? = null,
    disintegrationRequest: DisintegrationRequest<String>? = null,
) {
    val uiState by remember {
        LibraryRepository.ensureLoaded()
        LibraryRepository.uiState
    }.collectAsStateWithLifecycle()
    val libraryPosterPattern by remember {
        CustomPosterUrlRepository.ensureLoaded()
        CustomPosterUrlRepository.pattern
    }.collectAsStateWithLifecycle()
    val libraryPosterScreens by CustomPosterUrlRepository.enabledScreens.collectAsStateWithLifecycle()
    val effectivePosterPattern = remember(libraryPosterPattern, libraryPosterScreens) {
        if (CustomPosterScreen.LIBRARY in libraryPosterScreens) libraryPosterPattern else ""
    }
    val cloudUiState by CloudLibraryRepository.uiState.collectAsStateWithLifecycle()
    val cloudSettings by remember {
        DebridSettingsRepository.ensureLoaded()
        DebridSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val displaySettings by remember {
        LibraryDisplaySettingsRepository.ensureLoaded()
        LibraryDisplaySettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    var observedOfflineState by remember { mutableStateOf(false) }
    val serversUiState by remember {
        ServerRepository.ensureLoaded()
        ServerRepository.uiState
    }.collectAsStateWithLifecycle()
    val hasServers = serversUiState.connections.any { it.enabled }
    var sourceModeName by rememberSaveable { mutableStateOf(LibraryViewMode.Saved.name) }
    val sourceMode = remember(sourceModeName, hasServers) {
        runCatching { LibraryViewMode.valueOf(sourceModeName) }.getOrDefault(LibraryViewMode.Saved)
            .takeUnless { it == LibraryViewMode.Servers && !hasServers }
            ?: LibraryViewMode.Saved
    }
    var serverShelves by remember { mutableStateOf<List<LibraryServerShelf>?>(null) }
    var serverShelvesRequest by remember { mutableStateOf(0) }
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    var cloudSearchQuery by rememberSaveable { mutableStateOf("") }
    val selectedType = remember(selectedTypeName) {
        selectedTypeName?.let { runCatching { CloudLibraryItemType.valueOf(it) }.getOrNull() }
    }
    var selectedCloudItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var libraryGenre by rememberSaveable { mutableStateOf<String?>(null) }
    var libraryYear by rememberSaveable { mutableStateOf<String?>(null) }
    var libraryWatchedFilterName by rememberSaveable { mutableStateOf(LibraryWatchedFilter.ALL.name) }
    val libraryWatchedFilter = remember(libraryWatchedFilterName) {
        runCatching { LibraryWatchedFilter.valueOf(libraryWatchedFilterName) }.getOrDefault(LibraryWatchedFilter.ALL)
    }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    ScreenActivityEffect(listState) { screenActive ->
        if (!screenActive) listState.stopScroll()
    }
    val isRemoteSource = uiState.sourceMode != LibrarySourceMode.LOCAL
    val effectiveSortOption = effectiveLibrarySortOption(
        selected = displaySettings.sortOption,
        sourceMode = uiState.sourceMode,
    )
    val orderListKeys = if (sourceMode != LibraryViewMode.Saved) emptyList() else {
        if (displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) uiState.sections.map { it.type }
        else listOfNotNull(uiState.sections.firstOrNull { it.type == displaySettings.selectedSectionKey }?.type
            ?: uiState.sections.firstOrNull()?.type)
    }
    val providerOrders = rememberLibraryProviderOrders(uiState.sourceMode, orderListKeys, effectiveSortOption)
    val visibleSortOption = if (providerOrders.failed) LibrarySortOption.DEFAULT else effectiveSortOption
    val sortedSections = remember(uiState.sections, displaySettings, uiState.sourceMode, sourceMode, providerOrders) {
        if (sourceMode == LibraryViewMode.Saved && displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) {
            sortLibrarySections(
                sections = uiState.sections,
                selected = visibleSortOption,
                sourceMode = uiState.sourceMode,
                providerOrders = providerOrders.ranks,
            )
        } else {
            emptyList()
        }
    }
    val verticalProjection = remember(
        uiState.sections,
        uiState.sourceMode,
        libraryGenre,
        libraryYear,
        libraryWatchedFilter,
        watchedUiState.watchedKeys,
        fullyWatchedSeriesKeys,
        displaySettings,
        sourceMode,
        providerOrders,
    ) {
        if (sourceMode == LibraryViewMode.Saved && displaySettings.layoutMode == LibraryLayoutMode.VERTICAL) {
            buildLibraryVerticalProjection(
                sections = uiState.sections,
                sourceMode = uiState.sourceMode,
                selectedSectionKey = displaySettings.selectedSectionKey,
                selectedType = displaySettings.selectedType,
                sortOption = visibleSortOption,
                providerOrders = providerOrders.ranks,
                selectedGenre = libraryGenre,
                selectedYear = libraryYear,
                watchedFilter = libraryWatchedFilter,
                isWatched = { item ->
                    WatchingState.isPosterWatched(
                        watchedKeys = watchedUiState.watchedKeys,
                        item = item.toMetaPreview(),
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    )
                },
            )
        } else {
            LibraryVerticalProjection(
                availableSections = emptyList(),
                selectedSectionKey = null,
                availableTypes = emptyList(),
                selectedType = null,
                entries = emptyList(),
            )
        }
    }
    val retryLibraryLoad: () -> Unit = {
        NetworkStatusRepository.requestRefresh(force = true)
        coroutineScope.launch {
            LibraryRepository.pullFromServer(
                profileId = ProfileRepository.activeProfileId,
                refreshIntent = TrackingRefreshIntent.USER_INITIATED,
            )
        }
    }

    ScreenActivityEffect(networkStatusUiState.condition, isRemoteSource) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@ScreenActivityEffect
                observedOfflineState = false
                if (isRemoteSource) {
                    coroutineScope.launch {
                        LibraryRepository.pullFromServer(ProfileRepository.activeProfileId)
                    }
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    ScreenActivityEffect(scrollToTopRequests) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        scrollToTopRequests.collect {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(sourceMode, serversUiState.revision, serverShelvesRequest) {
        if (sourceMode == LibraryViewMode.Servers) serverShelves = loadServerShelves()
    }

    ScreenActivityEffect(sourceMode, cloudSettings.cloudLibraryEnabled, cloudSettings.providerApiKeys) { screenActive ->
        if (screenActive && sourceMode == LibraryViewMode.Cloud) {
            CloudLibraryRepository.ensureLoaded()
            selectedCloudItemKey = null
        }
    }

    val disintegration = remember { LibraryDisintegrationHolder() }
    val librarySectionsDisplay = if (
        sourceMode == LibraryViewMode.Saved &&
        displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL &&
        uiState.isLoaded &&
        sortedSections.isNotEmpty()
    ) {
        disintegration.sync(
            sourceMode = uiState.sourceMode,
            sections = sortedSections,
            previewLimit = LIBRARY_SECTION_PREVIEW_LIMIT,
            request = disintegrationRequest,
        )
    } else {
        disintegration.reset()
        emptyList()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val tokens = MaterialTheme.nuvio
        val posterCardStyle = rememberPosterCardStyleUiState()
        val pageHorizontalPadding = if (isDesktop) {
            desktopPageHorizontalPaddingForWidth(maxWidth.value)
        } else {
            16.dp
        }
        val useInlineDesktopToolbar = isDesktop && maxWidth >= 820.dp
        val gridColumns = remember(maxWidth, maxHeight, posterCardStyle.widthDp, isDesktop) {
            if (isDesktop) {
                posterGridColumnCountForViewport(maxWidth, maxHeight, posterCardStyle.widthDp)
            } else {
                posterGridColumnCountForWidth(maxWidth)
            }
        }
        val activeTitle = if (sourceMode != LibraryViewMode.Saved) {
            stringResource(Res.string.library_title)
        } else {
            when (uiState.sourceMode) {
                LibrarySourceMode.LOCAL -> stringResource(Res.string.library_title)
                LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_title)
                LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_title)
                LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_title)
            }
        }
        val activeItemCount = when (sourceMode) {
            LibraryViewMode.Saved -> when (displaySettings.layoutMode) {
                LibraryLayoutMode.HORIZONTAL -> uiState.sections.sumOf { it.items.size }
                LibraryLayoutMode.VERTICAL -> verticalProjection.entries.size
            }
            LibraryViewMode.Cloud -> cloudUiState.items.size
            LibraryViewMode.Servers -> serverShelves?.sumOf { it.section?.items?.size ?: 0 } ?: 0
        }

        NuvioScreen(
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = 0.dp,
            topPadding = if (topChromePadding != null) 0.dp else null,
            listState = listState,
        ) {
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nuvioHorizontalScrollBleed(LocalVerticalBarPadding.current),
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(tokens.colors.background)
                            .nuvioConsumePointerEvents(),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(LocalVerticalBarPadding.current),
                    ) {
                        if (isDesktop) {
                            DesktopLibraryToolbar(
                                title = activeTitle,
                                itemCount = activeItemCount,
                                sourceMode = sourceMode,
                                showServers = hasServers,
                                layoutMode = displaySettings.layoutMode,
                                inlineSourceSwitch = useInlineDesktopToolbar,
                                topPadding = topChromePadding,
                                horizontalPadding = pageHorizontalPadding,
                                onModeSelected = { mode -> sourceModeName = mode.name },
                                onToggleLayout = { targetLayout ->
                                    LibraryDisplaySettingsRepository.setLayoutMode(targetLayout)
                                },
                                onDownloadsClick = onDownloadsClick,
                            )
                        } else {
                            NuvioScreenHeader(
                                title = activeTitle,
                                backgroundColor = tokens.colors.background,
                                modifier = Modifier.padding(horizontal = pageHorizontalPadding),
                                topPadding = topChromePadding,
                                actions = {
                                    if (sourceMode == LibraryViewMode.Saved) {
                                        LibraryListManagementButton()
                                        val targetLayout = if (displaySettings.layoutMode == LibraryLayoutMode.HORIZONTAL) {
                                            LibraryLayoutMode.VERTICAL
                                        } else {
                                            LibraryLayoutMode.HORIZONTAL
                                        }
                                        IconButton(
                                            onClick = {
                                                LibraryDisplaySettingsRepository.setLayoutMode(targetLayout)
                                            },
                                        ) {
                                            Crossfade(
                                                targetState = targetLayout,
                                                animationSpec = tween(durationMillis = 140),
                                                label = "libraryLayoutAction",
                                            ) { animatedTargetLayout ->
                                                Icon(
                                                    imageVector = if (animatedTargetLayout == LibraryLayoutMode.VERTICAL) {
                                                        Icons.Rounded.GridView
                                                    } else {
                                                        Icons.Rounded.ViewAgenda
                                                    },
                                                    contentDescription = if (animatedTargetLayout == LibraryLayoutMode.VERTICAL) {
                                                        stringResource(Res.string.library_layout_show_vertical)
                                                    } else {
                                                        stringResource(Res.string.library_layout_show_horizontal)
                                                    },
                                                    tint = tokens.colors.textSecondary,
                                                )
                                            }
                                        }
                                    }
                                    if (onDownloadsClick != null) {
                                        LibraryDownloadsButton(onClick = onDownloadsClick)
                                    }
                                },
                            )
                            LibrarySourceSwitch(
                                selectedMode = sourceMode,
                                showServers = hasServers,
                                onModeSelected = { mode ->
                                    sourceModeName = mode.name
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = pageHorizontalPadding),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (isDesktop) {
                            HorizontalDivider(
                                thickness = NuvioTokens.Border.thin,
                                color = tokens.colors.borderSubtle,
                            )
                        }
                    }
                }
            }

            if (sourceMode == LibraryViewMode.Cloud) {
                cloudLibraryContent(
                    uiState = cloudUiState,
                    selectedProviderId = selectedProviderId,
                    selectedType = selectedType,
                    selectedCloudItemKey = selectedCloudItemKey,
                    searchQuery = cloudSearchQuery,
                    horizontalPadding = pageHorizontalPadding,
                    onSearchQueryChange = {
                        cloudSearchQuery = it
                        selectedCloudItemKey = null
                    },
                    onProviderSelected = {
                        selectedProviderId = it
                        selectedTypeName = null
                        selectedCloudItemKey = null
                    },
                    onTypeSelected = {
                        selectedTypeName = it?.name
                        selectedCloudItemKey = null
                    },
                    onItemSelected = { item ->
                        val playableFiles = item.playableFiles
                        when {
                            playableFiles.size == 1 -> onCloudFilePlay?.invoke(item, playableFiles.first())
                            playableFiles.size > 1 -> selectedCloudItemKey = item.stableKey
                        }
                    },
                    onFileSelected = { item, file -> onCloudFilePlay?.invoke(item, file) },
                    onBackToItems = { selectedCloudItemKey = null },
                    onRefresh = { CloudLibraryRepository.refresh() },
                    onConnectCloudClick = onConnectCloudClick,
                )
            } else if (sourceMode == LibraryViewMode.Servers) {
                serverLibraryContent(
                    shelves = serverShelves,
                    watchedKeys = watchedUiState.watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    onCatalogClick = onCatalogClick,
                    onPosterClick = onPreviewClick,
                    onPosterLongClick = onPreviewLongClick,
                    onRetry = { serverShelvesRequest++ },
                )
            } else {
                when {
                    !uiState.isLoaded || (uiState.isLoading && uiState.sections.isEmpty()) -> {
                        if (displaySettings.layoutMode == LibraryLayoutMode.VERTICAL) {
                            libraryVerticalGridSkeletonItems(
                                columns = gridColumns,
                                horizontalPadding = pageHorizontalPadding,
                            )
                        } else {
                            items(3) {
                                HomeSkeletonRow(
                                    horizontalPadding = pageHorizontalPadding,
                                )
                            }
                        }
                    }

                    !uiState.errorMessage.isNullOrBlank() && uiState.sections.isEmpty() -> {
                        item {
                            if (networkStatusUiState.isOfflineLike) {
                                NuvioNetworkOfflineCard(
                                    condition = networkStatusUiState.condition,
                                    modifier = Modifier.padding(horizontal = pageHorizontalPadding),
                                    onRetry = retryLibraryLoad,
                                )
                            } else {
                                HomeEmptyStateCard(
                                    modifier = Modifier.padding(horizontal = pageHorizontalPadding),
                                    title = when (uiState.sourceMode) {
                                        LibrarySourceMode.LOCAL -> stringResource(Res.string.library_load_failed)
                                        LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_load_failed)
                                        LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_load_failed)
                                        LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_load_failed)
                                    },
                                    message = uiState.errorMessage.orEmpty(),
                                    actionLabel = stringResource(Res.string.action_retry),
                                    onActionClick = retryLibraryLoad,
                                )
                            }
                        }
                    }

                    uiState.sections.isEmpty() -> {
                        item {
                            HomeEmptyStateCard(
                                modifier = Modifier.padding(horizontal = pageHorizontalPadding),
                                title = when (uiState.sourceMode) {
                                    LibrarySourceMode.LOCAL -> stringResource(Res.string.library_empty_title)
                                    LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_empty_title)
                                    LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_empty_title)
                                    LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_empty_title)
                                },
                                message = when (uiState.sourceMode) {
                                    LibrarySourceMode.LOCAL -> stringResource(Res.string.library_empty_message)
                                    LibrarySourceMode.TRAKT -> stringResource(Res.string.library_trakt_empty_message)
                                    LibrarySourceMode.SIMKL -> stringResource(Res.string.library_simkl_empty_message)
                                    LibrarySourceMode.MDBLIST -> stringResource(Res.string.library_mdblist_empty_message)
                                },
                            )
                        }
                    }

                    else -> {
                        item(
                            key = "library-saved-controls:${uiState.sourceMode}:" +
                                "${displaySettings.layoutMode}:$effectiveSortOption",
                        ) {
                            LibrarySavedControls(
                                layoutMode = displaySettings.layoutMode,
                                sourceMode = uiState.sourceMode,
                                sortOption = effectiveSortOption,
                                verticalProjection = verticalProjection,
                                watchedFilter = libraryWatchedFilter,
                                onSectionSelected = LibraryDisplaySettingsRepository::setSelectedSection,
                                onTypeSelected = LibraryDisplaySettingsRepository::setSelectedType,
                                onGenreSelected = { genre -> libraryGenre = genre },
                                onYearSelected = { year -> libraryYear = year },
                                onWatchedFilterSelected = { filter -> libraryWatchedFilterName = filter.name },
                                onSortSelected = LibraryDisplaySettingsRepository::setSortOption,
                                modifier = libraryContentTransitionModifier()
                                    .padding(horizontal = pageHorizontalPadding),
                            )
                        }
                        when (displaySettings.layoutMode) {
                            LibraryLayoutMode.HORIZONTAL -> librarySections(
                                displaySections = librarySectionsDisplay,
                                watchedKeys = watchedUiState.watchedKeys,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                sortOption = effectiveSortOption,
                                posterPattern = effectivePosterPattern,
                                horizontalPadding = pageHorizontalPadding,
                                onPosterClick = onPosterClick,
                                onSectionViewAllClick = onSectionViewAllClick,
                                onPosterLongClick = onPosterLongClick,
                                onDisintegrated = disintegration::onExited,
                            )
                            LibraryLayoutMode.VERTICAL -> libraryVerticalGridItems(
                                projection = verticalProjection,
                                columns = gridColumns,
                                horizontalPadding = pageHorizontalPadding,
                                watchedKeys = watchedUiState.watchedKeys,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                posterPattern = effectivePosterPattern,
                                onPosterClick = onPosterClick,
                                onPosterLongClick = onPosterLongClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopLibraryToolbar(
    title: String,
    itemCount: Int,
    sourceMode: LibraryViewMode,
    showServers: Boolean,
    layoutMode: LibraryLayoutMode,
    inlineSourceSwitch: Boolean,
    topPadding: Dp?,
    horizontalPadding: Dp,
    onModeSelected: (LibraryViewMode) -> Unit,
    onToggleLayout: (LibraryLayoutMode) -> Unit,
    onDownloadsClick: (() -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val resolvedTopPadding = topPadding ?: statusBarTop

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = resolvedTopPadding,
                bottom = NuvioTokens.Space.s12,
            ),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.displayLarge,
                    color = tokens.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (itemCount > 0) {
                    Surface(
                        shape = tokens.shapes.chip,
                        color = tokens.colors.surfaceElevated,
                        border = BorderStroke(NuvioTokens.Border.thin, tokens.colors.borderSubtle),
                    ) {
                        Text(
                            text = stringResource(Res.string.collections_trakt_list_items_count, itemCount),
                            modifier = Modifier.padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s4),
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (inlineSourceSwitch) {
                    LibrarySourceSwitch(
                        selectedMode = sourceMode,
                        showServers = showServers,
                        onModeSelected = onModeSelected,
                    )
                    Spacer(modifier = Modifier.width(NuvioTokens.Space.s4))
                }
                if (sourceMode == LibraryViewMode.Saved) {
                    LibraryListManagementButton()
                    val targetLayout = if (layoutMode == LibraryLayoutMode.HORIZONTAL) {
                        LibraryLayoutMode.VERTICAL
                    } else {
                        LibraryLayoutMode.HORIZONTAL
                    }
                    val layoutInteraction = remember { MutableInteractionSource() }
                    val isLayoutHovered by layoutInteraction.collectIsHoveredAsState()
                    val layoutDesc = if (targetLayout == LibraryLayoutMode.VERTICAL) {
                        stringResource(Res.string.library_layout_show_vertical)
                    } else {
                        stringResource(Res.string.library_layout_show_horizontal)
                    }
                    Surface(
                        onClick = { onToggleLayout(targetLayout) },
                        modifier = Modifier
                            .size(40.dp)
                            .nuvioFocusBorder(CircleShape)
                            .pointerHoverIcon(PointerIcon.Hand),
                        shape = CircleShape,
                        color = if (isLayoutHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
                        border = BorderStroke(
                            width = NuvioTokens.Border.thin,
                            color = if (isLayoutHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
                        ),
                        interactionSource = layoutInteraction,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Crossfade(
                                targetState = targetLayout,
                                animationSpec = tween(durationMillis = 140),
                                label = "libraryLayoutActionDesktop",
                            ) { animatedTargetLayout ->
                                Icon(
                                    imageVector = if (animatedTargetLayout == LibraryLayoutMode.VERTICAL) {
                                        Icons.Rounded.GridView
                                    } else {
                                        Icons.Rounded.ViewAgenda
                                    },
                                    contentDescription = layoutDesc,
                                    tint = if (isLayoutHovered) tokens.colors.textPrimary else tokens.colors.textSecondary,
                                    modifier = Modifier.size(NuvioTokens.Icon.md),
                                )
                            }
                        }
                    }
                }
                if (onDownloadsClick != null) {
                    LibraryDownloadsButton(onClick = onDownloadsClick)
                }
                FullscreenActionButton(
                    buttonSize = 40.dp,
                    iconSize = 22.dp,
                    contentColor = tokens.colors.textPrimary,
                )
            }
        }

        if (!inlineSourceSwitch) {
            LibrarySourceSwitch(
                selectedMode = sourceMode,
                showServers = showServers,
                onModeSelected = onModeSelected,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyListScope.cloudLibraryContent(
    uiState: CloudLibraryUiState,
    selectedProviderId: String?,
    selectedType: CloudLibraryItemType?,
    selectedCloudItemKey: String?,
    searchQuery: String,
    horizontalPadding: Dp,
    onSearchQueryChange: (String) -> Unit,
    onProviderSelected: (String?) -> Unit,
    onTypeSelected: (CloudLibraryItemType?) -> Unit,
    onItemSelected: (CloudLibraryItem) -> Unit,
    onFileSelected: (CloudLibraryItem, CloudLibraryFile) -> Unit,
    onBackToItems: () -> Unit,
    onRefresh: () -> Unit,
    onConnectCloudClick: (() -> Unit)?,
) {
    when {
        !uiState.isLoaded -> {
            cloudLibrarySkeletonItems(horizontalPadding = horizontalPadding)
        }

        !uiState.isEnabled -> {
            item {
                CloudLibraryEmptyStateCard(
                    horizontalPadding = horizontalPadding,
                    title = stringResource(Res.string.cloud_library_disabled_title),
                    message = stringResource(Res.string.cloud_library_disabled_message),
                    actionLabel = stringResource(Res.string.cloud_library_disabled_action),
                    onActionClick = onConnectCloudClick,
                )
            }
        }

        !uiState.hasConnectedProvider -> {
            item {
                CloudLibraryEmptyStateCard(
                    horizontalPadding = horizontalPadding,
                    title = stringResource(Res.string.cloud_library_connect_title),
                    message = stringResource(Res.string.cloud_library_connect_message),
                    actionLabel = stringResource(Res.string.cloud_library_connect_action),
                    onActionClick = onConnectCloudClick,
                )
            }
        }

        else -> {
            val providerItems = uiState.items
                .filter { item -> selectedProviderId == null || item.providerId == selectedProviderId }
            val availableTypes = providerItems
                .map { item -> item.type }
                .distinct()
                .sortedBy { type -> type.ordinal }
            val effectiveSelectedType = selectedType?.takeIf { type -> type in availableTypes }
            val typeFilteredItems = providerItems
                .filter { item -> effectiveSelectedType == null || item.type == effectiveSelectedType }
            // Local filter over the already-loaded library. Matches the item name or any of its
            // file names, since the useful identifier is often in the filename, not the title.
            val trimmedQuery = searchQuery.trim()
            val hasActiveFilter = selectedProviderId != null || effectiveSelectedType != null || trimmedQuery.isNotEmpty()
            val filteredItems = if (trimmedQuery.isEmpty()) {
                typeFilteredItems
            } else {
                typeFilteredItems.filter { item ->
                    item.name.contains(trimmedQuery, ignoreCase = true) ||
                        item.files.any { file -> file.name.contains(trimmedQuery, ignoreCase = true) }
                }
            }
            val selectedItem = filteredItems.firstOrNull { it.stableKey == selectedCloudItemKey }

            if (selectedItem != null) {
                item {
                    CloudLibraryFilePicker(
                        item = selectedItem,
                        onBack = onBackToItems,
                        onFileSelected = { file -> onFileSelected(selectedItem, file) },
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                    )
                }
            } else {
                item {
                    CloudLibraryToolbar(
                        uiState = uiState,
                        selectedProviderId = selectedProviderId,
                        selectedType = effectiveSelectedType,
                        availableTypes = availableTypes,
                        onProviderSelected = onProviderSelected,
                        onTypeSelected = onTypeSelected,
                        onRefresh = onRefresh,
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                    )
                }

                item(key = "cloud-library-search") {
                    CloudLibrarySearchField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp),
                    )
                }

                val visibleProviderStates = uiState.providers.filter { providerState ->
                    selectedProviderId == null || providerState.providerId == selectedProviderId
                }
                val failedProviderStates = visibleProviderStates.filter { providerState ->
                    !providerState.errorMessage.isNullOrBlank() && providerState.items.isEmpty()
                }
                failedProviderStates.forEach { providerState ->
                    item(key = "cloud-error-${providerState.providerId}") {
                        HomeEmptyStateCard(
                            modifier = Modifier.padding(horizontal = horizontalPadding),
                            title = stringResource(Res.string.cloud_library_load_failed, providerState.providerName),
                            message = providerState.errorMessage.orEmpty(),
                            actionLabel = stringResource(Res.string.action_retry),
                            onActionClick = onRefresh,
                        )
                    }
                }

                if (uiState.isRefreshing && filteredItems.isEmpty()) {
                    cloudLibrarySkeletonItems(horizontalPadding = horizontalPadding)
                } else if (filteredItems.isEmpty() && failedProviderStates.isEmpty()) {
                    item {
                        HomeEmptyStateCard(
                            modifier = Modifier.padding(horizontal = horizontalPadding),
                            title = stringResource(
                                if (hasActiveFilter) {
                                    Res.string.cloud_library_no_matches_title
                                } else {
                                    Res.string.cloud_library_empty_title
                                },
                            ),
                            message = stringResource(
                                if (hasActiveFilter) {
                                    Res.string.cloud_library_no_matches_message
                                } else {
                                    Res.string.cloud_library_empty_message
                                },
                            ),
                            actionLabel = if (hasActiveFilter) null else stringResource(Res.string.action_retry),
                            onActionClick = if (hasActiveFilter) null else onRefresh,
                        )
                    }
                } else {
                    items(
                        items = filteredItems,
                        key = { item -> item.stableKey },
                    ) { item ->
                        CloudLibraryRow(
                            item = item,
                            onClick = { onItemSelected(item) },
                            modifier = Modifier.padding(horizontal = horizontalPadding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudLibraryEmptyStateCard(
    horizontalPadding: Dp,
    title: String,
    message: String,
    actionLabel: String?,
    onActionClick: (() -> Unit)?,
) {
    if (isDesktop) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            HomeEmptyStateCard(
                modifier = Modifier.widthIn(max = 520.dp),
                title = title,
                message = message,
                actionLabel = actionLabel,
                onActionClick = onActionClick,
            )
        }
    } else {
        HomeEmptyStateCard(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            title = title,
            message = message,
            actionLabel = actionLabel,
            onActionClick = onActionClick,
        )
    }
}

@Composable
private fun CloudLibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(14.dp)
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> tokens.colors.accent
            isDesktop && isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderDefault
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "cloud_search_border",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (isDesktop) 50.dp else 46.dp)
            .clip(shape)
            .background(if (isFocused) tokens.colors.surfaceCard else tokens.colors.surfaceElevated)
            .border(NuvioTokens.Border.thin, borderColor, shape)
            .then(if (isDesktop) Modifier.hoverable(interactionSource) else Modifier)
            .padding(horizontal = NuvioTokens.Space.s14),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = if (isFocused) tokens.colors.accent else tokens.colors.textMuted,
            modifier = Modifier.size(NuvioTokens.Icon.md),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(Res.string.cloud_library_search_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = tokens.colors.textPrimary,
                    textDirection = TextDirection.Content,
                ),
                cursorBrush = SolidColor(tokens.colors.accent),
            )
        }
        if (query.isNotEmpty()) {
            val clearInteraction = remember { MutableInteractionSource() }
            val isClearHovered by clearInteraction.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDesktop && isClearHovered) tokens.colors.surfaceElevated else tokens.colors.overlayHover)
                    .then(
                        if (isDesktop) {
                            Modifier
                                .hoverable(clearInteraction)
                                .pointerHoverIcon(PointerIcon.Hand)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = clearInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = { onQueryChange("") },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.compose_search_clear),
                    tint = if (isDesktop && isClearHovered) tokens.colors.textPrimary else tokens.colors.textMuted,
                    modifier = Modifier.size(NuvioTokens.Icon.sm),
                )
            }
        }
    }
}

private fun LazyListScope.cloudLibrarySkeletonItems(horizontalPadding: Dp) {
    item(key = "cloud-library-skeleton-toolbar") {
        CloudLibrarySkeletonToolbar(
            modifier = Modifier.padding(horizontal = horizontalPadding),
        )
    }
    items(3) {
        CloudLibrarySkeletonRow(
            modifier = Modifier.padding(horizontal = horizontalPadding),
        )
    }
}

@Composable
private fun LibrarySourceSwitch(
    selectedMode: LibraryViewMode,
    showServers: Boolean,
    onModeSelected: (LibraryViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryChip(
            label = stringResource(Res.string.library_source_saved),
            selected = selectedMode == LibraryViewMode.Saved,
            onClick = { onModeSelected(LibraryViewMode.Saved) },
        )
        LibraryChip(
            label = stringResource(Res.string.library_source_cloud),
            selected = selectedMode == LibraryViewMode.Cloud,
            onClick = { onModeSelected(LibraryViewMode.Cloud) },
        )
        if (showServers) {
            LibraryChip(
                label = stringResource(Res.string.library_source_servers),
                selected = selectedMode == LibraryViewMode.Servers,
                onClick = { onModeSelected(LibraryViewMode.Servers) },
            )
        }
    }
}

@Composable
private fun CloudLibraryToolbar(
    uiState: CloudLibraryUiState,
    selectedProviderId: String?,
    selectedType: CloudLibraryItemType?,
    availableTypes: List<CloudLibraryItemType>,
    onProviderSelected: (String?) -> Unit,
    onTypeSelected: (CloudLibraryItemType?) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val providerOptions = buildList {
        add(NuvioDropdownOption(key = "", label = stringResource(Res.string.cloud_library_provider_all)))
        addAll(
            uiState.providers.map { provider ->
                NuvioDropdownOption(
                    key = provider.providerId,
                    label = provider.providerName,
                )
            },
        )
    }
    val typeOptions = buildList {
        add(NuvioDropdownOption(key = "", label = stringResource(Res.string.cloud_library_type_all)))
        addAll(
            availableTypes.map { type ->
                NuvioDropdownOption(
                    key = type.name,
                    label = cloudLibraryTypeLabel(type),
                )
            },
        )
    }
    val selectedProviderName = uiState.providers
        .firstOrNull { provider -> provider.providerId == selectedProviderId }
        ?.providerName
        ?: stringResource(Res.string.cloud_library_provider_all)
    val selectedTypeLabel = selectedType?.let { type -> cloudLibraryTypeLabel(type) }
        ?: stringResource(Res.string.cloud_library_type_all)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NuvioDropdownChip(
                    title = stringResource(Res.string.cloud_library_select_provider),
                    label = selectedProviderName,
                    selectedKey = selectedProviderId.orEmpty(),
                    options = providerOptions,
                    enabled = providerOptions.size > 1,
                    onSelected = { option ->
                        onProviderSelected(option.key.ifBlank { null })
                    },
                )
                NuvioDropdownChip(
                    title = stringResource(Res.string.cloud_library_select_type),
                    label = selectedTypeLabel,
                    selectedKey = selectedType?.name.orEmpty(),
                    options = typeOptions,
                    enabled = typeOptions.size > 1,
                    onSelected = { option ->
                        val type = option.key
                            .takeIf { it.isNotBlank() }
                            ?.let(CloudLibraryItemType::valueOf)
                        onTypeSelected(type)
                    },
                )
            }
            val refreshInteraction = remember { MutableInteractionSource() }
            val isRefreshHovered by refreshInteraction.collectIsHoveredAsState()
            Surface(
                onClick = onRefresh,
                modifier = Modifier
                    .size(38.dp)
                    .nuvioFocusBorder(CircleShape)
                    .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                shape = CircleShape,
                color = if (isDesktop && isRefreshHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
                border = BorderStroke(
                    width = NuvioTokens.Border.thin,
                    color = if (isDesktop && isRefreshHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
                ),
                interactionSource = refreshInteraction,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(Res.string.cloud_library_refresh),
                        tint = if (isDesktop && isRefreshHovered) tokens.colors.textPrimary else tokens.colors.textSecondary,
                        modifier = Modifier.size(NuvioTokens.Icon.sm + NuvioTokens.Space.s2),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryChip(
    label: String,
    selected: Boolean,
    loading: Boolean = false,
    error: Boolean = false,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val pillShape = RoundedCornerShape(NuvioTokens.Radius.full)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> tokens.colors.focusBackground
            isDesktop && isHovered -> tokens.colors.surfaceCard
            else -> tokens.colors.surfaceElevated
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "library_chip_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            error -> tokens.colors.danger
            selected -> tokens.colors.accent
            isDesktop && isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderDefault
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "library_chip_border",
    )

    Box(
        modifier = Modifier
            .nuvioFocusBorder(pillShape)
            .clip(pillShape)
            .background(containerColor)
            .border(NuvioTokens.Border.thin, borderColor, pillShape)
            .semantics { this.selected = selected }
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
            .padding(horizontal = NuvioTokens.Space.s14, vertical = NuvioTokens.Space.s8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            modifier = Modifier.shimmer(loading),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = when {
                error -> tokens.colors.danger
                selected || (isDesktop && isHovered) -> tokens.colors.textPrimary
                else -> tokens.colors.textSecondary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CloudLibraryRow(
    item: CloudLibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val playableCount = item.playableFiles.size
    val enabled = playableCount > 0
    val cardShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && enabled && isHovered) {
            tokens.colors.borderStrong
        } else {
            tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "cloud_row_border",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (isDesktop && enabled) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        shape = cardShape,
        color = if (isDesktop && enabled && isHovered) tokens.colors.surfaceElevated else tokens.colors.surfaceCard,
        border = BorderStroke(NuvioTokens.Border.thin, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NuvioTokens.Space.s16, vertical = NuvioTokens.Space.s12),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s2),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = cloudLibrarySubtitle(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = cloudLibraryStatusLine(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (enabled) {
                    Spacer(modifier = Modifier.width(NuvioTokens.Space.s10))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(tokens.colors.surfaceElevated)
                            .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(Res.string.action_play),
                            tint = tokens.colors.accent,
                            modifier = Modifier.size(NuvioTokens.Icon.md),
                        )
                    }
                }
            }
            item.progressFraction?.takeIf { it in 0f..0.999f }?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NuvioTokens.Radius.full)),
                    color = tokens.colors.accent,
                    trackColor = tokens.colors.surfaceElevated,
                )
            }
        }
    }
}

@Composable
private fun CloudLibraryFilePicker(
    item: CloudLibraryItem,
    onBack: () -> Unit,
    onFileSelected: (CloudLibraryFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = cardShape,
        color = tokens.colors.surfaceCard,
        border = BorderStroke(NuvioTokens.Border.thin, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NuvioTokens.Space.s16, vertical = NuvioTokens.Space.s14),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
            ) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                    shape = CircleShape,
                    color = tokens.colors.surfaceElevated,
                    border = BorderStroke(NuvioTokens.Border.thin, tokens.colors.borderDefault),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                            tint = tokens.colors.textPrimary,
                            modifier = Modifier.size(NuvioTokens.Icon.md),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(Res.string.cloud_library_file_picker_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textSecondary,
                    )
                }
            }

            val files = item.playableFiles
            if (files.isEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.cloud_library_no_files_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.colors.textPrimary,
                    )
                    Text(
                        text = stringResource(Res.string.cloud_library_no_files_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textSecondary,
                    )
                }
            } else {
                files.forEach { file ->
                    CloudLibraryFileRow(
                        file = file,
                        onClick = { onFileSelected(file) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudLibraryFileRow(
    file: CloudLibraryFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val rowShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clip(rowShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = rowShape,
        color = if (isDesktop && isHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
        border = BorderStroke(
            width = NuvioTokens.Border.thin,
            color = if (isDesktop && isHovered) tokens.colors.borderStrong else tokens.colors.borderSubtle,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NuvioTokens.Space.s14, vertical = NuvioTokens.Space.s12),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
            ) {
                Icon(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp),
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = file.sizeBytes?.let { size -> formatCloudBytes(size) }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(Res.string.cloud_library_play_file),
                    tint = tokens.colors.accent,
                )
            }
        }
    }
}

@Composable
private fun cloudLibrarySubtitle(item: CloudLibraryItem): String {
    val fileLine = when (val playableCount = item.playableFiles.size) {
        0 -> stringResource(Res.string.cloud_library_no_playable_files)
        1 -> item.playableFiles.first().name
        else -> stringResource(Res.string.cloud_library_playable_file_count, playableCount)
    }
    return listOf(item.providerName, cloudLibraryTypeLabel(item.type), fileLine).joinToString(" • ")
}

@Composable
private fun cloudLibraryStatusLine(item: CloudLibraryItem): String {
    val fallback = if (item.playableFiles.isEmpty()) {
        stringResource(Res.string.cloud_library_no_playable_files)
    } else {
        stringResource(Res.string.cloud_library_status_ready)
    }
    return listOfNotNull(
        item.status?.toDisplayStatus(),
        item.sizeBytes?.let(::formatCloudBytes),
        item.progressFraction?.let { "${(it * 100f).toInt()}%" },
    ).joinToString(" • ").ifBlank { fallback }
}

@Composable
private fun cloudLibraryTypeLabel(type: CloudLibraryItemType): String =
    when (type) {
        CloudLibraryItemType.Torrent -> stringResource(Res.string.cloud_library_type_torrents)
        CloudLibraryItemType.Usenet -> stringResource(Res.string.cloud_library_type_usenet)
        CloudLibraryItemType.WebDownload -> stringResource(Res.string.cloud_library_type_web)
        CloudLibraryItemType.File -> stringResource(Res.string.cloud_library_type_files)
    }

private fun formatCloudBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 ${localizedByteUnit("B")}"
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gib -> "${((value / gib) * 10.0).toInt() / 10.0} ${localizedByteUnit("GB")}"
        value >= mib -> "${((value / mib) * 10.0).toInt() / 10.0} ${localizedByteUnit("MB")}"
        value >= kib -> "${((value / kib) * 10.0).toInt() / 10.0} ${localizedByteUnit("KB")}"
        else -> "$bytes ${localizedByteUnit("B")}"
    }
}

private fun String.toDisplayStatus(): String =
    replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.titlecase() }

@Composable
private fun CloudLibrarySkeletonToolbar(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBlock(width = 112.dp, height = 36.dp, cornerRadius = 12.dp)
            SkeletonBlock(width = 92.dp, height = 36.dp, cornerRadius = 12.dp)
        }
    }
}

@Composable
private fun CloudLibrarySkeletonRow(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = cardShape,
        color = tokens.colors.surfaceCard,
        border = BorderStroke(NuvioTokens.Border.thin, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NuvioTokens.Space.s16, vertical = NuvioTokens.Space.s12),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SkeletonBlock(
                        modifier = Modifier.fillMaxWidth(0.74f),
                        height = 18.dp,
                        cornerRadius = 6.dp,
                    )
                    SkeletonBlock(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        height = 14.dp,
                        cornerRadius = 6.dp,
                    )
                    SkeletonBlock(
                        modifier = Modifier.fillMaxWidth(0.52f),
                        height = 12.dp,
                        cornerRadius = 6.dp,
                    )
                }
                SkeletonBlock(width = 36.dp, height = 36.dp, cornerRadius = 18.dp)
            }
        }
    }
}

private enum class LibraryViewMode {
    Saved,
    Cloud,
    Servers,
}

private fun LazyListScope.libraryVerticalGridItems(
    projection: LibraryVerticalProjection,
    columns: Int,
    horizontalPadding: Dp,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    posterPattern: String,
    onPosterClick: ((LibraryItem) -> Unit)?,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)?,
) {
    items(
        items = projection.entries.chunked(columns),
        key = { rowEntries ->
            val firstEntry = rowEntries.first()
            "library-vertical:${firstEntry.item.type}:${firstEntry.item.id}"
        },
    ) { rowEntries ->
        val resolvedItems = remember(rowEntries, posterPattern) {
            rowEntries.map { entry ->
                entry.item.toMetaPreview().let {
                    if (posterPattern.isNotBlank()) it.withCustomPosterUrl(posterPattern) else it
                }
            }
        }
        PosterGridRow(
            items = resolvedItems,
            columns = columns,
            modifier = libraryContentTransitionModifier()
                .padding(horizontal = horizontalPadding),
            watchedKeys = watchedKeys,
            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
            onPosterClick = onPosterClick?.let { callback ->
                { preview ->
                    rowEntries.firstOrNull { it.item.id == preview.id && it.item.type == preview.type }
                        ?.item
                        ?.let(callback)
                }
            },
            onPosterLongClick = onPosterLongClick?.let { callback ->
                { preview ->
                    rowEntries.firstOrNull { it.item.id == preview.id && it.item.type == preview.type }
                        ?.let { entry -> callback(entry.item, entry.section) }
                }
            },
        )
    }
}

private fun LazyListScope.libraryVerticalGridSkeletonItems(
    columns: Int,
    horizontalPadding: Dp,
) {
    items(
        count = 2,
        key = { index -> "library-vertical-skeleton:$index" },
    ) {
        PosterGridSkeletonRow(
            columns = columns,
            modifier = libraryContentTransitionModifier()
                .padding(horizontal = horizontalPadding),
        )
    }
}

private fun LazyListScope.librarySections(
    displaySections: List<LibraryDisplaySection>,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    sortOption: LibrarySortOption,
    posterPattern: String,
    horizontalPadding: Dp,
    onPosterClick: ((LibraryItem) -> Unit)?,
    onSectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)?,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)?,
    onDisintegrated: (String) -> Unit,
) {
    items(
        items = displaySections,
        key = { section -> "library-horizontal:${section.type}" },
    ) { section ->
        NuvioShelfSection(
            title = localizedLibrarySectionTitle(section.type, section.displayTitle),
            entries = section.previewEntries,
            modifier = libraryContentTransitionModifier(),
            headerHorizontalPadding = horizontalPadding,
            rowContentPadding = PaddingValues(horizontal = horizontalPadding),
            onViewAllClick = section.source
                ?.takeIf { it.items.size > LIBRARY_SECTION_PREVIEW_LIMIT }
                ?.let { source -> onSectionViewAllClick?.let { { it(source, sortOption) } } },
            viewAllPillSize = NuvioViewAllPillSize.Compact,
            key = { entry -> entry.globalKey },
            animatePlacement = true,
        ) { entry ->
            val item = entry.item
            val posterItem = remember(item, posterPattern) {
                item.toMetaPreview().let {
                    if (posterPattern.isNotBlank()) it.withCustomPosterUrl(posterPattern) else it
                }
            }
            val entrySource = entry.section
            DisintegratingContainer(
                disintegrating = entry.exiting,
                onDisintegrated = { onDisintegrated(entry.globalKey) },
            ) {
                HomePosterCard(
                    item = posterItem,
                    isWatched = WatchingState.isPosterWatched(
                        watchedKeys = watchedKeys,
                        item = posterItem,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    ),
                    onClick = if (entry.exiting) null else onPosterClick?.let { { it(item) } },
                    onLongClick = if (entry.exiting || entrySource == null) {
                        null
                    } else {
                        onPosterLongClick?.let { { it(item, entrySource) } }
                    },
                )
            }
        }
    }
}

private const val LIBRARY_SECTION_PREVIEW_LIMIT = 18

private data class LibraryDisplayEntry(
    val globalKey: String,
    val item: LibraryItem,
    val section: LibrarySection?,
    val exiting: Boolean,
)

private data class LibraryDisplaySection(
    val source: LibrarySection?,
    val type: String,
    val displayTitle: String,
    val previewEntries: List<LibraryDisplayEntry>,
)

private class LibraryExitingEntry(
    val item: LibraryItem,
    val sectionType: String,
    val sectionTitle: String,
    val index: Int,
)

private class LibraryDisintegrationHolder {
    private val tracker = ScopedDisintegrationTracker<LibrarySourceMode, String, LibraryExitingEntry> { entry ->
        librarySectionItemKey(entry.sectionType, entry.item)
    }

    fun onExited(globalKey: String) {
        tracker.onDisintegrated(globalKey)
    }

    fun reset() {
        tracker.reset()
    }

    fun sync(
        sourceMode: LibrarySourceMode,
        sections: List<LibrarySection>,
        previewLimit: Int,
        request: DisintegrationRequest<String>?,
    ): List<LibraryDisplaySection> {
        val current = ArrayList<LibraryExitingEntry>()
        sections.forEach { section ->
            section.items.take(previewLimit).forEachIndexed { index, item ->
                current += LibraryExitingEntry(item, section.type, section.displayTitle, index)
            }
        }
        val exitingBySection = tracker.sync(sourceMode, current, request)
            .asSequence()
            .filter { entry -> entry.exiting }
            .map { entry -> entry.item }
            .groupBy { entry -> entry.sectionType }
        val seenTypes = HashSet<String>(sections.size)
        val result = ArrayList<LibraryDisplaySection>(sections.size + 1)

        for (section in sections) {
            seenTypes += section.type
            val entries = ArrayList<LibraryDisplayEntry>(previewLimit + 1)
            section.items.take(previewLimit).forEach { item ->
                entries += LibraryDisplayEntry(
                    globalKey = librarySectionItemKey(section.type, item),
                    item = item,
                    section = section,
                    exiting = false,
                )
            }
            exitingBySection[section.type]?.sortedBy { it.index }?.forEach { ex ->
                val key = librarySectionItemKey(section.type, ex.item)
                if (entries.none { it.globalKey == key }) {
                    entries.add(
                        ex.index.coerceIn(0, entries.size),
                        LibraryDisplayEntry(key, ex.item, section, exiting = true),
                    )
                }
            }
            result += LibraryDisplaySection(section, section.type, section.displayTitle, entries)
        }

        for ((type, list) in exitingBySection) {
            if (type in seenTypes) continue
            val sorted = list.sortedBy { it.index }
            val entries = sorted.map { ex ->
                LibraryDisplayEntry(librarySectionItemKey(type, ex.item), ex.item, section = null, exiting = true)
            }
            result += LibraryDisplaySection(null, type, sorted.first().sectionTitle, entries)
        }

        return result
    }
}
