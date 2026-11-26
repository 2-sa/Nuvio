package com.nuvio.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.deeplink.handleAppUrl
import com.nuvio.app.core.ui.DisintegrationRequest
import com.nuvio.app.core.ui.FullscreenActionButton
import com.nuvio.app.core.ui.LocalDesktopHistoryActions
import com.nuvio.app.core.ui.LocalNuvioNavBarScrollState
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioNavBarScrollState
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.isFullscreenActionSupported
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioTrackTopBarFocus
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.controller.ControllerFocusZone
import com.nuvio.app.controller.controllerFocusable
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.cloud.CloudLibraryContentType
import com.nuvio.app.features.cloud.CloudLibraryFile
import com.nuvio.app.features.cloud.CloudLibraryItem
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.HomeScreen
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibraryScreen
import com.nuvio.app.features.library.LibrarySection
import com.nuvio.app.features.library.LibrarySortOption
import com.nuvio.app.features.player.PlayerBackReleaseGuard
import com.nuvio.app.features.player.PlayerBackRequest
import com.nuvio.app.features.profiles.ActiveProfileMiniAvatar
import com.nuvio.app.features.profiles.AvatarCatalogItem
import com.nuvio.app.features.profiles.AvatarRepository
import com.nuvio.app.features.profiles.NuvioProfile
import com.nuvio.app.features.profiles.ProfileBackgroundBackdrop
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.profiles.ProfileSwitcherTab
import com.nuvio.app.features.profiles.SidebarProfileSwitcherStack
import com.nuvio.app.features.search.SearchHistoryRepository
import com.nuvio.app.features.search.SearchRepository
import com.nuvio.app.features.search.SearchScreen
import com.nuvio.app.features.settings.AppBrandWordmark
import com.nuvio.app.features.settings.NavBarStyle
import com.nuvio.app.features.settings.SettingsScreen
import com.nuvio.app.features.settings.ThemeSettingsRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.isDesktop
import com.nuvio.app.navigation.AppRoute
import com.nuvio.app.navigation.NuvioNavigator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.flow.Flow
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_brand_name
import nuvio.composeapp.generated.resources.compose_nav_home
import nuvio.composeapp.generated.resources.compose_nav_library
import nuvio.composeapp.generated.resources.compose_nav_profile
import nuvio.composeapp.generated.resources.compose_nav_search
import nuvio.composeapp.generated.resources.compose_nav_settings
import nuvio.composeapp.generated.resources.compose_search_clear
import nuvio.composeapp.generated.resources.compose_search_placeholder
import nuvio.composeapp.generated.resources.compose_settings_page_root
import nuvio.composeapp.generated.resources.sidebar_library
import nuvio.composeapp.generated.resources.sidebar_search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val DesktopSidebarCollapsedWidth = 72.dp
internal val DesktopSidebarExpandedWidth = 224.dp
private val DesktopSidebarExpandedContentWidth = 192.dp
private val DesktopSidebarItemHeight = 48.dp
private val DesktopSidebarIconSlotSize = 38.dp
private val DesktopSidebarIconSize = NuvioTokens.Icon.lg

internal fun desktopSidebarWidthForState(expanded: Boolean): Dp =
    if (expanded) DesktopSidebarExpandedWidth else DesktopSidebarCollapsedWidth

internal fun desktopSidebarIsExpanded(
    navBarStyle: NavBarStyle,
    isHovered: Boolean,
    profileStackVisible: Boolean,
): Boolean = when (navBarStyle) {
    NavBarStyle.EXPANDED -> true
    NavBarStyle.COMPACT -> profileStackVisible
    NavBarStyle.ADAPTIVE, NavBarStyle.CLASSIC -> isHovered || profileStackVisible
}

internal fun desktopSidebarActiveIndicatorAlpha(selected: Boolean): Float =
    if (selected) 1f else 0f

@Composable
internal fun rememberGuardedPopBackStack(
    navController: NuvioNavigator,
    route: AppRoute,
    beforePop: () -> Unit = {},
): () -> Unit {
    var popHandled by remember(route) { mutableStateOf(false) }

    return remember(navController, route, popHandled, beforePop) {
        {
            if (!popHandled && navController.currentRoute == route) {
                popHandled = true
                beforePop()
                navController.popBackStack(expectedRoute = route)
            }
        }
    }
}

internal data class AppTabState(
    val searchListState: LazyListState,
    val homeContentGeneration: Int = 0,
    val profileId: Int? = null,
    val searchFocusRequestCount: Int = 0,
    val tabsRouteActiveState: State<Boolean>,
    val topChromePadding: Dp? = null,
    val searchQuery: String? = null,
    val onSearchQueryChange: ((String) -> Unit)? = null,
    val libraryDisintegrationRequest: DisintegrationRequest<String>? = null,
    val continueWatchingDisintegrationRequest: DisintegrationRequest<String>? = null,
    val requestedSettingsPageName: String? = null,
)

internal data class AppTabRequests(
    val homeScrollToTopRequests: Flow<Unit>,
    val searchScrollToTopRequests: Flow<Unit>,
    val libraryScrollToTopRequests: Flow<Unit>,
    val settingsRootActionRequests: Flow<Unit>,
)

internal data class AppTabActions(
    val onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    val onPosterClick: ((MetaPreview) -> Unit)? = null,
    val onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    val onLibraryPosterClick: ((LibraryItem) -> Unit)? = null,
    val onLibraryPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)? = null,
    val onLibrarySectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)? = null,
    val onCloudFilePlay: ((CloudLibraryItem, CloudLibraryFile) -> Unit)? = null,
    val onConnectCloudClick: (() -> Unit)? = null,
    val onDownloadsClick: () -> Unit = {},
    val onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
    val onContinueWatchingLongPress: ((ContinueWatchingItem) -> Unit)? = null,
    val onSwitchProfile: (() -> Unit)? = null,
    val onSettingsPageClick: ((pageName: String, title: String) -> Unit)? = null,
    val onHomescreenSettingsClick: () -> Unit = {},
    val onMetaScreenSettingsClick: () -> Unit = {},
    val onContinueWatchingSettingsClick: () -> Unit = {},
    val onAddonsSettingsClick: () -> Unit = {},
    val onPluginsSettingsClick: () -> Unit = {},
    val onAccountSettingsClick: () -> Unit = {},
    val onSupportersContributorsSettingsClick: () -> Unit = {},
    val onLicensesAttributionsSettingsClick: () -> Unit = {},
    val onCheckForUpdatesClick: (() -> Unit)? = null,
    val onTestUpdateBannerClick: (() -> Unit)? = null,
    val onCollectionsSettingsClick: () -> Unit = {},
    val onWatchPartySettingsClick: () -> Unit = {},
    val onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
    val onRequestedSettingsPageConsumed: () -> Unit = {},
    val onInitialHomeContentRendered: () -> Unit = {},
)

@Composable
internal fun rememberGuardedPlayerPopBackStack(
    navController: NuvioNavigator,
    route: AppRoute,
    beforePop: () -> Unit = {},
): PlayerBackRequest {
    val guard = remember(route) { PlayerBackReleaseGuard() }

    return remember(navController, route, beforePop, guard) {
        { releaseBeforeBack ->
            guard.request(
                canStart = {
                    navController.currentRoute == route &&
                        navController.canPopBackStack(expectedRoute = route)
                },
                releaseBeforeBack = releaseBeforeBack,
                beforePop = beforePop,
                pop = {
                    navController.currentRoute == route &&
                        navController.popBackStack(expectedRoute = route)
                },
            )
        }
    }
}

@Composable
internal fun AppTabHost(
    selectedTab: AppScreenTab,
    requests: AppTabRequests,
    state: AppTabState,
    actions: AppTabActions,
    modifier: Modifier = Modifier,
) {
    RootTabHost(
        selectedTab = selectedTab,
        modifier = modifier,
        active = state.tabsRouteActiveState.value,
        profileId = state.profileId,
    ) { tab ->
        // The content entry must live on the selected screen, not on the tab host: RootTabHost keeps
        // previously visited tabs composed but unplaced, and focusing a container whose first child
        // is an unplaced pane leaves focus on the container itself (no ring, no way out with arrows).
        val contentModifier = Modifier.fillMaxSize()
        when (tab) {
            AppScreenTab.Home -> {
                key(state.homeContentGeneration) {
                    HomeScreen(
                        modifier = contentModifier,
                        topChromePadding = state.topChromePadding,
                        animateCollectionGifs = state.tabsRouteActiveState.value && selectedTab == AppScreenTab.Home,
                        scrollToTopRequests = requests.homeScrollToTopRequests,
                        onCatalogClick = actions.onCatalogClick,
                        onPosterClick = actions.onPosterClick,
                        onPosterLongClick = actions.onPosterLongClick,
                        onContinueWatchingClick = actions.onContinueWatchingClick,
                        onContinueWatchingLongPress = actions.onContinueWatchingLongPress,
                        continueWatchingDisintegrationRequest = state.continueWatchingDisintegrationRequest,
                        onFolderClick = actions.onFolderClick,
                        onFirstCatalogRendered = actions.onInitialHomeContentRendered,
                    )
                }
            }

            AppScreenTab.Search -> {
                SearchScreen(
                    modifier = contentModifier,
                    topChromePadding = state.topChromePadding,
                    listState = state.searchListState,
                    onPosterClick = actions.onPosterClick,
                    onPosterLongClick = actions.onPosterLongClick,
                    searchFocusRequestCount = state.searchFocusRequestCount,
                    scrollToTopRequests = requests.searchScrollToTopRequests,
                    externalQuery = state.searchQuery,
                    onExternalQueryChange = state.onSearchQueryChange,
                )
            }

            AppScreenTab.Library -> {
                LibraryScreen(
                    modifier = contentModifier,
                    topChromePadding = state.topChromePadding,
                    scrollToTopRequests = requests.libraryScrollToTopRequests,
                    onPosterClick = actions.onLibraryPosterClick,
                    onPosterLongClick = actions.onLibraryPosterLongClick,
                    onSectionViewAllClick = actions.onLibrarySectionViewAllClick,
                    onCloudFilePlay = actions.onCloudFilePlay,
                    onConnectCloudClick = actions.onConnectCloudClick,
                    onCatalogClick = actions.onCatalogClick,
                    onPreviewClick = actions.onPosterClick,
                    onPreviewLongClick = actions.onPosterLongClick,
                    onDownloadsClick = actions.onDownloadsClick,
                    disintegrationRequest = state.libraryDisintegrationRequest,
                )
            }

            AppScreenTab.Settings -> {
                SettingsScreen(
                    modifier = contentModifier,
                    topChromePadding = state.topChromePadding,
                    rootActionRequests = requests.settingsRootActionRequests,
                    requestedPageName = state.requestedSettingsPageName,
                    onRequestedPageConsumed = actions.onRequestedSettingsPageConsumed,
                    rootActionsEnabled = state.tabsRouteActiveState.value,
                    isSelectedTab = selectedTab == AppScreenTab.Settings,
                    onNavigatePage = actions.onSettingsPageClick,
                    onSwitchProfile = actions.onSwitchProfile,
                    onHomescreenClick = actions.onHomescreenSettingsClick,
                    onMetaScreenClick = actions.onMetaScreenSettingsClick,
                    onContinueWatchingClick = actions.onContinueWatchingSettingsClick,
                    onAddonsClick = actions.onAddonsSettingsClick,
                    onPluginsClick = actions.onPluginsSettingsClick,
                    onAccountClick = actions.onAccountSettingsClick,
                    onSupportersContributorsClick = actions.onSupportersContributorsSettingsClick,
                    onLicensesAttributionsClick = actions.onLicensesAttributionsSettingsClick,
                    onCheckForUpdatesClick = actions.onCheckForUpdatesClick,
                    onTestUpdateBannerClick = actions.onTestUpdateBannerClick,
                    onCollectionsClick = actions.onCollectionsSettingsClick,
                    onWatchPartyClick = actions.onWatchPartySettingsClick,
                )
            }
        }
    }
}


@Composable
internal fun TabletFloatingTopBar(
    selectedTab: AppScreenTab,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
    navBarStyleSetting: NavBarStyle = NavBarStyle.ADAPTIVE,
    isHeroEnabled: Boolean = true,
    hazeState: HazeState? = null,
    scrollState: NuvioNavBarScrollState? = null,
    windowWidth: Dp? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isProfileSwitcherOpen by remember { mutableStateOf(false) }

    val isNarrowWindow = windowWidth != null && windowWidth < 800.dp
    val isLargeWindow = windowWidth != null && windowWidth > 1600.dp

    val hasSettingsDrawer = selectedTab == AppScreenTab.Settings && (windowWidth == null || windowWidth >= 960.dp)
    val supportsScrollFrostedHaze = selectedTab == AppScreenTab.Home || hasSettingsDrawer
    val scrollThreshold = if (selectedTab == AppScreenTab.Settings) 70f else 35f
    val isScrolledAwayFromTop = scrollState != null && scrollState.totalScrollOffset > scrollThreshold
    val isScrolled = supportsScrollFrostedHaze && isScrolledAwayFromTop
    val isFrosted = isScrolled || isHovered || isProfileSwitcherOpen

    val surfaceColor by animateColorAsState(
        targetValue = if (isFrosted) {
            Color(0xFF1C1C1E).copy(alpha = 0.30f)
        } else {
            Color(0xFF0F0F11).copy(alpha = 0.20f)
        },
        animationSpec = tween(
            durationMillis = 320,
            easing = FastOutSlowInEasing,
        ),
        label = "top_bar_surface_color",
    )

    val isHeroPresentOnHome = selectedTab == AppScreenTab.Home && isHeroEnabled

    val targetLabelFraction = when (navBarStyleSetting) {
        NavBarStyle.EXPANDED -> 1f
        NavBarStyle.COMPACT -> 0f
        else -> { // ADAPTIVE
            if (isHovered || isProfileSwitcherOpen) {
                1f
            } else if (isHeroPresentOnHome) {
                // Home with hero enabled: compact at top and when scrolled (unless hovered)
                0f
            } else if (selectedTab == AppScreenTab.Home) {
                // Home with hero disabled: expanded at top, collapses to compact when scrolled down
                if (isScrolledAwayFromTop) 0f else 1f
            } else {
                // Library, Search/Discover, Settings: ALWAYS expanded in Adaptive mode
                1f
            }
        }
    }
    val labelFraction by animateFloatAsState(
        targetValue = targetLabelFraction,
        animationSpec = tween(
            durationMillis = 320,
            easing = FastOutSlowInEasing,
        ),
        label = "top_bar_label_fraction",
    )

    val frostedSheenAlpha by animateFloatAsState(
        targetValue = if (isFrosted) 1f else 0f,
        animationSpec = tween(
            durationMillis = 320,
            easing = FastOutSlowInEasing,
        ),
        label = "top_bar_sheen_alpha",
    )

    val pillHeight = when {
        isNarrowWindow -> 34.dp
        isLargeWindow -> 42.dp
        else -> 38.dp
    }
    val navIconSize = when {
        isNarrowWindow -> 16.dp
        isLargeWindow -> 20.dp
        else -> NuvioTokens.Space.s18
    }
    val avatarSize = when {
        isNarrowWindow -> 24
        isLargeWindow -> 30
        else -> 28
    }
    val iconCollapsedPadding = when {
        isNarrowWindow -> 9.dp
        isLargeWindow -> 11.dp
        else -> 10.dp
    }
    val avatarCollapsedPadding = when {
        isNarrowWindow -> 5.dp
        isLargeWindow -> 6.dp
        else -> 5.dp
    }
    val avatarExpandedStartPadding = when {
        isNarrowWindow -> 6.dp
        isLargeWindow -> 10.dp
        else -> 8.dp
    }
    val avatarExpandedEndPadding = when {
        isNarrowWindow -> 14.dp
        isLargeWindow -> 18.dp
        else -> 16.dp
    }
    val expandedHorizontalPadding = when {
        isNarrowWindow -> 10.dp
        isLargeWindow -> 14.dp
        else -> 12.dp
    }
    val collapsedItemSpacing = when {
        isNarrowWindow -> 4.dp
        isLargeWindow -> 8.dp
        else -> 6.dp
    }
    val expandedItemSpacing = when {
        isNarrowWindow -> 4.dp
        isLargeWindow -> 8.dp
        else -> tokens.spacing.controlGap
    }
    val itemSpacing = collapsedItemSpacing * (1f - labelFraction) + expandedItemSpacing * labelFraction
    val baseTopPadding = when {
        isNarrowWindow -> 6.dp
        isLargeWindow -> 14.dp
        else -> NuvioTokens.Space.s10
    }

    val labelTextStyle = when {
        isNarrowWindow -> MaterialTheme.typography.labelMedium
        isLargeWindow -> MaterialTheme.typography.titleSmall
        else -> MaterialTheme.typography.labelLarge
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .nuvioTrackTopBarFocus()
            .padding(top = statusBarPadding + baseTopPadding, bottom = tokens.spacing.controlGap),
        contentAlignment = Alignment.TopCenter,
    ) {
        val chipShape = tokens.shapes.chip
        val pillModifier = Modifier
            .clip(chipShape)
            .then(
                if (isFrosted && hazeState != null) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = 14.dp
                    }
                } else {
                    Modifier
                },
            )
            .hoverable(interactionSource)

        Surface(
            color = surfaceColor,
            shape = chipShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = pillModifier,
        ) {
            Box {
                if (frostedSheenAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(chipShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.14f * frostedSheenAlpha),
                                        Color.White.copy(alpha = 0.03f * frostedSheenAlpha),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                }

                Row(
                    modifier = Modifier.padding(
                        horizontal = if (isNarrowWindow) 8.dp else if (isLargeWindow) 12.dp else NuvioTokens.Space.s10,
                        vertical = if (isNarrowWindow) 4.dp else tokens.spacing.controlGap,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabletTopPillItem(
                        label = stringResource(Res.string.compose_nav_home),
                        selected = selectedTab == AppScreenTab.Home,
                        onClick = { onTabSelected(AppScreenTab.Home) },
                        labelFraction = labelFraction,
                        pillHeight = pillHeight,
                        expandedHorizontalPadding = expandedHorizontalPadding,
                        collapsedHorizontalPadding = iconCollapsedPadding,
                        textStyle = labelTextStyle,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = stringResource(Res.string.compose_nav_home),
                                modifier = Modifier.size(navIconSize),
                                tint = if (selectedTab == AppScreenTab.Home) {
                                    tokens.colors.textPrimary
                                } else {
                                    Color.White.copy(alpha = 0.70f)
                                },
                            )
                        },
                    )
                    TabletTopPillItem(
                        label = stringResource(Res.string.compose_nav_search),
                        selected = selectedTab == AppScreenTab.Search,
                        onClick = { onTabSelected(AppScreenTab.Search) },
                        labelFraction = labelFraction,
                        pillHeight = pillHeight,
                        expandedHorizontalPadding = expandedHorizontalPadding,
                        collapsedHorizontalPadding = iconCollapsedPadding,
                        textStyle = labelTextStyle,
                        icon = {
                            Icon(
                                painter = painterResource(Res.drawable.sidebar_search),
                                contentDescription = stringResource(Res.string.compose_nav_search),
                                modifier = Modifier.size(navIconSize),
                                tint = if (selectedTab == AppScreenTab.Search) {
                                    tokens.colors.textPrimary
                                } else {
                                    Color.White.copy(alpha = 0.70f)
                                },
                            )
                        },
                    )
                    TabletTopPillItem(
                        label = stringResource(Res.string.compose_nav_library),
                        selected = selectedTab == AppScreenTab.Library,
                        onClick = { onTabSelected(AppScreenTab.Library) },
                        labelFraction = labelFraction,
                        pillHeight = pillHeight,
                        expandedHorizontalPadding = expandedHorizontalPadding,
                        collapsedHorizontalPadding = iconCollapsedPadding,
                        textStyle = labelTextStyle,
                        icon = {
                            Icon(
                                painter = painterResource(Res.drawable.sidebar_library),
                                contentDescription = stringResource(Res.string.compose_nav_library),
                                modifier = Modifier.size(navIconSize),
                                tint = if (selectedTab == AppScreenTab.Library) {
                                    tokens.colors.textPrimary
                                } else {
                                    Color.White.copy(alpha = 0.70f)
                                },
                            )
                        },
                    )
                    TabletTopPillItem(
                        label = stringResource(Res.string.compose_nav_settings),
                        selected = selectedTab == AppScreenTab.Settings,
                        onClick = { onTabSelected(AppScreenTab.Settings) },
                        labelFraction = labelFraction,
                        pillHeight = pillHeight,
                        expandedHorizontalPadding = expandedHorizontalPadding,
                        collapsedHorizontalPadding = avatarCollapsedPadding,
                        expandedStartPadding = avatarExpandedStartPadding,
                        expandedEndPadding = avatarExpandedEndPadding,
                        textStyle = labelTextStyle,
                        icon = {
                            ProfileSwitcherTab(
                                selected = selectedTab == AppScreenTab.Settings,
                                onClick = { onTabSelected(AppScreenTab.Settings) },
                                onProfileSelected = onProfileSelected,
                                onAddProfileRequested = onAddProfileRequested,
                                onPopupStateChanged = { isProfileSwitcherOpen = it },
                                avatarSize = avatarSize,
                                hazeState = hazeState,
                                popupAlignment = Alignment.TopCenter,
                            )
                        },
                    )
                }
            }
        }
    }
}

internal fun ContinueWatchingItem.isCloudLibraryContinueWatchingItem(): Boolean =
    parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)

@Composable
private fun TabletTopPillItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    labelFraction: Float = 1f,
    pillHeight: Dp = 38.dp,
    expandedHorizontalPadding: Dp = 12.dp,
    collapsedHorizontalPadding: Dp = 10.dp,
    expandedStartPadding: Dp = expandedHorizontalPadding,
    expandedEndPadding: Dp = expandedHorizontalPadding,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    icon: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val selectedBgColor by animateColorAsState(
        targetValue = if (selected) tokens.colors.accent.copy(alpha = 0.18f) else Color.Transparent,
        label = "pill_bg_color",
    )
    val itemShape = tokens.shapes.chip

    val startPadding = if (labelFraction > 0.05f) expandedStartPadding else collapsedHorizontalPadding
    val endPadding = if (labelFraction > 0.05f) expandedEndPadding else collapsedHorizontalPadding

    Surface(
        onClick = onClick,
        color = selectedBgColor,
        shape = itemShape,
        tonalElevation = if (selected) tokens.elevation.raised else tokens.elevation.flat,
        modifier = Modifier
            .height(pillHeight)
            .clip(itemShape)
            .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = startPadding, end = endPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            AnimatedVisibility(
                visible = labelFraction > 0.05f,
                enter = expandHorizontally(
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Start,
                ) + fadeIn(tween(250)),
                exit = shrinkHorizontally(
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Start,
                ) + fadeOut(tween(200)),
            ) {
                Text(
                    text = label,
                    style = textStyle,
                    color = if (selected) {
                        tokens.colors.textPrimary
                    } else {
                        Color.White.copy(alpha = 0.75f)
                    },
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
internal fun AppLoadingContent(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppBrandWordmark(
                contentDescription = stringResource(Res.string.app_brand_name),
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .height(44.dp),
            )
            Spacer(modifier = Modifier.height(tokens.spacing.sectionGap))
            NuvioLoadingIndicator()
        }
    }
}

@Composable
internal fun AppLaunchOverlay(
    profile: NuvioProfile?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.zIndex(NuvioTokens.Z.dialog),
    ) {
        ProfileBackgroundBackdrop(
            profile = profile,
            modifier = Modifier.fillMaxSize(),
        )
        AppLoadingContent(modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun DesktopHoverSidebar(
    selectedTab: AppScreenTab,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
    sidebarExpanded: Boolean = false,
    sidebarWidth: Dp = DesktopSidebarCollapsedWidth,
    hoverSource: MutableInteractionSource = remember { MutableInteractionSource() },
    profileStackVisible: Boolean = false,
    onProfileStackVisibleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    val activeProfile = profileState.activeProfile
    val activeProfileName = activeProfile?.name ?: stringResource(Res.string.compose_nav_profile)
    val brandTopPadding = statusBarPadding + 18.dp
    fun selectTab(tab: AppScreenTab) {
        onProfileStackVisibleChange(false)
        onTabSelected(tab)
    }

    val sidebarSurfaceColor by animateColorAsState(
        targetValue = if (sidebarExpanded) {
            tokens.colors.surfaceElevated.copy(alpha = 0.92f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "desktop_sidebar_surface",
    )
    val collapsedScrimAlpha by animateFloatAsState(
        targetValue = if (sidebarExpanded) 0f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "desktop_sidebar_scrim",
    )
    val expandedBorderAlpha by animateFloatAsState(
        targetValue = if (sidebarExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "desktop_sidebar_border",
    )

    Surface(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .hoverable(hoverSource)
            .zIndex(NuvioTokens.Z.navigation),
        color = sidebarSurfaceColor,
        contentColor = tokens.colors.textPrimary,
        border = if (expandedBorderAlpha > 0.01f) {
            BorderStroke(1.dp, tokens.colors.borderSubtle.copy(alpha = 0.5f * expandedBorderAlpha))
        } else {
            null
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (collapsedScrimAlpha > 0.01f) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.56f * collapsedScrimAlpha),
                                    Color.Black.copy(alpha = 0.24f * collapsedScrimAlpha),
                                    Color.Transparent,
                                ),
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .background(
                    Brush.verticalGradient(
                        0.0f to tokens.colors.accent.copy(alpha = 0.04f),
                        0.38f to Color.Transparent,
                        1.0f to Color.Transparent,
                    ),
                ),
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = brandTopPadding, start = 10.dp, end = 10.dp)
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                AnimatedVisibility(
                    visible = !sidebarExpanded,
                    enter = fadeIn(animationSpec = tween(160)),
                    exit = fadeOut(animationSpec = tween(100)),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 7.dp)
                            .size(DesktopSidebarIconSlotSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        val emblemShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(emblemShape)
                                .background(tokens.colors.focusBackground, emblemShape)
                                .border(
                                    width = 1.dp,
                                    color = tokens.colors.accent.copy(alpha = 0.35f),
                                    shape = emblemShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "N",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    brush = palette.accentBrush(),
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = sidebarExpanded,
                    enter = fadeIn(animationSpec = tween(180, delayMillis = 40)),
                    exit = fadeOut(animationSpec = tween(100)),
                ) {
                    AppBrandWordmark(
                        contentDescription = stringResource(Res.string.app_brand_name),
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .height(24.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DesktopSidebarItem(
                    id = "sidebar_home",
                    label = stringResource(Res.string.compose_nav_home),
                    selected = selectedTab == AppScreenTab.Home,
                    expanded = sidebarExpanded,
                    onClick = { selectTab(AppScreenTab.Home) },
                ) { color ->
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = stringResource(Res.string.compose_nav_home),
                        modifier = Modifier.size(DesktopSidebarIconSize),
                        tint = color,
                    )
                }
                DesktopSidebarItem(
                    id = "sidebar_search",
                    label = stringResource(Res.string.compose_nav_search),
                    selected = selectedTab == AppScreenTab.Search,
                    expanded = sidebarExpanded,
                    onClick = { selectTab(AppScreenTab.Search) },
                ) { color ->
                    Icon(
                        painter = painterResource(Res.drawable.sidebar_search),
                        contentDescription = stringResource(Res.string.compose_nav_search),
                        modifier = Modifier.size(DesktopSidebarIconSize),
                        tint = color,
                    )
                }
                DesktopSidebarItem(
                    id = "sidebar_library",
                    label = stringResource(Res.string.compose_nav_library),
                    selected = selectedTab == AppScreenTab.Library,
                    expanded = sidebarExpanded,
                    onClick = { selectTab(AppScreenTab.Library) },
                ) { color ->
                    Icon(
                        painter = painterResource(Res.drawable.sidebar_library),
                        contentDescription = stringResource(Res.string.compose_nav_library),
                        modifier = Modifier.size(DesktopSidebarIconSize),
                        tint = color,
                    )
                }
                DesktopSidebarItem(
                    id = "sidebar_settings",
                    label = stringResource(Res.string.compose_settings_page_root),
                    selected = selectedTab == AppScreenTab.Settings,
                    expanded = sidebarExpanded,
                    onClick = { selectTab(AppScreenTab.Settings) },
                ) { color ->
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(Res.string.compose_settings_page_root),
                        modifier = Modifier.size(DesktopSidebarIconSize),
                        tint = color,
                    )
                }
            }

            if (profileStackVisible) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.colors.surfacePopover.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, tokens.colors.borderDefault),
                    shadowElevation = tokens.elevation.overlay,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp + DesktopSidebarItemHeight + 8.dp)
                        .width(DesktopSidebarExpandedContentWidth)
                        .zIndex(NuvioTokens.Z.sheet),
                ) {
                    SidebarProfileSwitcherStack(
                        onProfileSelected = onProfileSelected,
                        onAddProfileRequested = onAddProfileRequested,
                        onDismissRequest = { onProfileStackVisibleChange(false) },
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }

            val profileShape = RoundedCornerShape(14.dp)
            val profileInteractionSource = remember { MutableInteractionSource() }
            val isProfileHovered by profileInteractionSource.collectIsHoveredAsState()
            val isProfilePillVisible = sidebarExpanded || (isDesktop && isProfileHovered) || profileStackVisible
            val profileBg by animateColorAsState(
                targetValue = if (isProfilePillVisible) {
                    tokens.colors.surfaceCard.copy(alpha = 0.72f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                label = "desktop_sidebar_profile_bg",
            )
            val profileBorderColor by animateColorAsState(
                targetValue = if (isProfilePillVisible) {
                    tokens.colors.borderSubtle
                } else {
                    Color.Transparent
                },
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                label = "desktop_sidebar_profile_border",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 18.dp)
                    .fillMaxWidth()
                    .height(DesktopSidebarItemHeight)
                    .clip(profileShape)
                    .background(profileBg, profileShape)
                    .then(
                        if (profileBorderColor.alpha > 0.01f) {
                            Modifier.border(width = 1.dp, color = profileBorderColor, shape = profileShape)
                        } else {
                            Modifier
                        },
                    )
                    .nuvioFocusBorder(profileShape)
                    .hoverable(profileInteractionSource)
                    .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                    .clickable(
                        interactionSource = profileInteractionSource,
                        indication = null,
                        onClick = { onProfileStackVisibleChange(!profileStackVisible) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                DesktopSidebarProfileTrigger(
                    profile = activeProfile,
                    avatars = avatars,
                    label = activeProfileName,
                    expanded = sidebarExpanded,
                )
            }
        }
    }
}

@Composable
private fun DesktopSidebarProfileTrigger(
    profile: NuvioProfile?,
    avatars: List<AvatarCatalogItem>,
    label: String,
    expanded: Boolean,
) {
    val tokens = MaterialTheme.nuvio

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(DesktopSidebarIconSlotSize),
                contentAlignment = Alignment.Center,
            ) {
                ActiveProfileMiniAvatar(
                    profile = profile,
                    avatars = avatars,
                    selected = false,
                    size = 32,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(160)) + expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                ),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.Start,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                ) + fadeOut(
                    animationSpec = tween(80, delayMillis = 120),
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.colors.textPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopSidebarItem(
    id: String,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val itemShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hovered = isDesktop && isHovered

    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> tokens.colors.focusBackground
            hovered -> tokens.colors.overlayHover
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "desktop_sidebar_item_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) tokens.colors.accent.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "desktop_sidebar_item_border",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected || hovered) tokens.colors.textPrimary else tokens.colors.textMuted,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "desktop_sidebar_item_fg",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(DesktopSidebarItemHeight)
            .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .controllerFocusable(
                id = id,
                zone = ControllerFocusZone.Navigation,
                isDefaultFocus = selected,
                shape = itemShape,
                cornerRadius = 14.dp,
                scaleOnFocus = 1.03f,
                selectLabel = label,
                onSelect = onClick,
            )
            .nuvioFocusBorder(itemShape),
        color = containerColor,
        border = if (borderColor.alpha > 0.01f) BorderStroke(1.dp, borderColor) else null,
        shape = itemShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(DesktopSidebarIconSlotSize),
                contentAlignment = Alignment.Center,
            ) {
                icon(contentColor)
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(160)) + expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                ),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.Start,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                ) + fadeOut(
                    animationSpec = tween(80, delayMillis = 120),
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DesktopFloatingTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
    startPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val focusManager = LocalFocusManager.current
    val historyActions = LocalDesktopHistoryActions.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    val recentSearches by SearchHistoryRepository.uiState.collectAsStateWithLifecycle()
    val activeProfile = profileState.activeProfile
    val activeProfileName = activeProfile?.name ?: stringResource(Res.string.compose_nav_profile)

    var isSearchFocused by remember { mutableStateOf(false) }
    var isProfileMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SearchHistoryRepository.ensureLoaded()
    }

    fun executeSearch(rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) {
            SearchRepository.clear()
            return
        }
        if (trimmed.startsWith("nuvio://", ignoreCase = true) || trimmed.startsWith("stremio://", ignoreCase = true)) {
            handleAppUrl(trimmed)
            onSearchQueryChange("")
            focusManager.clearFocus()
            return
        }
        onTabSelected(AppScreenTab.Search)
        SearchRepository.search(
            query = trimmed,
            addons = AddonRepository.uiState.value.addons,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(NuvioTokens.Z.navigation + 1f)
            .nuvioTrackTopBarFocus()
            .padding(
                top = statusBarPadding + 14.dp,
                start = startPadding + 16.dp,
                end = 24.dp,
            ),
    ) {
        // Left: Back / Forward navigation buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopFloatingHistoryButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Back",
                enabled = historyActions.canGoBack && historyActions.onNavigateBack != null,
                onClick = { historyActions.onNavigateBack?.invoke() },
            )
            DesktopFloatingHistoryButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Forward",
                enabled = historyActions.canGoForward && historyActions.onNavigateForward != null,
                onClick = { historyActions.onNavigateForward?.invoke() },
            )
        }

        // Center: Translucent Floating Search Pill + recent searches dropdown
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pillShape = RoundedCornerShape(50)
            val pillBorderColor by animateColorAsState(
                targetValue = if (isSearchFocused) tokens.colors.borderDefault else tokens.colors.borderSubtle,
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                label = "desktop_floating_search_border",
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = pillShape,
                color = tokens.colors.surfaceElevated.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, pillBorderColor),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = if (isSearchFocused) tokens.colors.textPrimary else tokens.colors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Text) else Modifier),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.compose_search_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { updated ->
                                onSearchQueryChange(updated)
                                if (updated.isNotBlank()) {
                                    onTabSelected(AppScreenTab.Search)
                                } else {
                                    SearchRepository.clear()
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = tokens.colors.textPrimary,
                            ),
                            cursorBrush = SolidColor(tokens.colors.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    executeSearch(searchQuery)
                                    focusManager.clearFocus()
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isSearchFocused = it.isFocused },
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                                .clickable {
                                    onSearchQueryChange("")
                                    SearchRepository.clear()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(Res.string.compose_search_clear),
                                tint = tokens.colors.textMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            if (isSearchFocused && searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                DesktopFloatingSearchDropdown(
                    recentSearches = recentSearches.take(6),
                    onSelectSearch = { term ->
                        onSearchQueryChange(term)
                        executeSearch(term)
                        focusManager.clearFocus()
                    },
                )
            }
        }

        // Right: Fullscreen toggle + active Profile avatar pill
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
        ) {
            Row(
                modifier = Modifier.height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFullscreenActionSupported) {
                    Surface(
                        shape = CircleShape,
                        color = tokens.colors.surfaceElevated.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
                        modifier = Modifier
                            .size(36.dp)
                            .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                    ) {
                        FullscreenActionButton(
                            buttonSize = 36.dp,
                            iconSize = 18.dp,
                            containerColor = Color.Transparent,
                            contentColor = tokens.colors.textPrimary,
                        )
                    }
                }

                val profilePillShape = RoundedCornerShape(50)
                Surface(
                    onClick = { isProfileMenuOpen = !isProfileMenuOpen },
                    shape = profilePillShape,
                    color = tokens.colors.surfaceElevated.copy(alpha = 0.72f),
                    border = BorderStroke(
                        1.dp,
                        if (isProfileMenuOpen) tokens.colors.borderDefault else tokens.colors.borderSubtle,
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 5.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ActiveProfileMiniAvatar(
                            profile = activeProfile,
                            avatars = avatars,
                            selected = false,
                            size = 26,
                        )
                        Text(
                            text = activeProfileName,
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.colors.textPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (isProfileMenuOpen) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.colors.surfacePopover.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, tokens.colors.borderDefault),
                    shadowElevation = tokens.elevation.overlay,
                    modifier = Modifier.width(216.dp),
                ) {
                    SidebarProfileSwitcherStack(
                        onProfileSelected = { profile ->
                            isProfileMenuOpen = false
                            onProfileSelected(profile)
                        },
                        onAddProfileRequested = {
                            isProfileMenuOpen = false
                            onAddProfileRequested()
                        },
                        onDismissRequest = { isProfileMenuOpen = false },
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopFloatingHistoryButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = tokens.colors.surfaceElevated.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
        modifier = Modifier
            .size(36.dp)
            .then(if (isDesktop && enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tokens.colors.textPrimary else tokens.colors.textMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DesktopFloatingSearchDropdown(
    recentSearches: List<String>,
    onSelectSearch: (String) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tokens.colors.surfacePopover.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, tokens.colors.borderDefault),
        shadowElevation = tokens.elevation.overlay,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            recentSearches.forEach { term ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                        .clickable { onSelectSearch(term) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = tokens.colors.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = term,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
