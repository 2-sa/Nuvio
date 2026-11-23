package com.nuvio.app.features.settings

import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.isDesktop

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.ContentEntry
import com.nuvio.app.core.ui.LocalNuvioBottomNavigationOverlayPadding
import com.nuvio.app.core.ui.LocalScreenActive
import com.nuvio.app.core.ui.NuvioDesktopVerticalScrollbar
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.core.ui.desktopSmoothWheelScroll
import com.nuvio.app.core.ui.isLiquidGlassNativeTabBarSupported
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioContentEntry
import com.nuvio.app.core.ui.nuvioTypeScale
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.details.MetaScreenSettingsUiState
import com.nuvio.app.core.ui.PosterCardStyleRepository
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.features.collection.CollectionRepository
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.firstEnabledManifestError
import com.nuvio.app.features.addons.hasPendingEnabledManifests
import com.nuvio.app.features.addons.isWaitingForFirstEnabledManifest
import com.nuvio.app.features.debrid.DebridSettings
import com.nuvio.app.features.debrid.DebridSettingsRepository
import com.nuvio.app.features.home.HomeCatalogSettingsItem
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.buildAddonCatalogRefreshSignature
import com.nuvio.app.features.mdblist.MdbListSettings
import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.features.mdblist.MdbListTracker
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsUiState

import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.simkl.SimklAuthRepository
import com.nuvio.app.features.simkl.SimklAuthUiState
import com.nuvio.app.features.trakt.TraktAuthUiState
import com.nuvio.app.features.trakt.TraktAuthRepository
import com.nuvio.app.features.trakt.TraktCommentsSettings
import com.nuvio.app.features.tracking.TrackingSettingsRepository
import com.nuvio.app.features.tracking.TrackingSettingsUiState
import com.nuvio.app.features.tmdb.TmdbSettings
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesUiState
import com.nuvio.app.navigation.LocalUseNativeNavigation
import com.nuvio.app.features.party.PartyRepository
import com.nuvio.app.features.p2p.P2pSettingsRepository
import com.nuvio.app.isIos
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_root
import nuvio.composeapp.generated.resources.settings_search_results_for
import nuvio.composeapp.generated.resources.settings_search_results_section
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val SettingsSearchRevealThreshold = 28.dp
private const val SettingsSearchRevealAnimationMillis = 240L
private const val SettingsSearchRevealHapticDelayMillis = 90L

@Composable
private fun watchPartyRow(onClick: () -> Unit): (() -> Unit)? {
    val available by PartyRepository.available.collectAsStateWithLifecycle()
    return onClick.takeIf { available }
}

private fun SettingsPage.isEnabledByPolicy(): Boolean =
    when (this) {
        SettingsPage.SupportersContributors -> AppFeaturePolicy.supportersContributorsPageEnabled
        SettingsPage.PlaybackP2p -> P2pSettingsRepository.isVisible
        SettingsPage.PlaybackBuffer -> !isIos && !isDesktop
        SettingsPage.Notifications -> AppFeaturePolicy.notificationsEnabled
        else -> true
    }

@Composable
private fun settingsPageTitles(): Map<SettingsPage, String> {
    val titles = mutableMapOf<SettingsPage, String>()
    for (page in SettingsPage.entries) {
        titles[page] = stringResource(page.titleRes)
    }
    return titles
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    topChromePadding: Dp? = null,
    rootActionRequests: Flow<Unit> = emptyFlow(),
    initialPageName: String = SettingsPage.Root.name,
    requestedPageName: String? = null,
    onRequestedPageConsumed: () -> Unit = {},
    rootActionsEnabled: Boolean = true,
    isSelectedTab: Boolean = true,
    onNavigatePage: ((pageName: String, title: String) -> Unit)? = null,
    onExternalBack: (() -> Unit)? = null,
    showInternalHeader: Boolean = true,
    onSwitchProfile: (() -> Unit)? = null,
    onHomescreenClick: () -> Unit = {},
    onMetaScreenClick: () -> Unit = {},
    onContinueWatchingClick: () -> Unit = {},
    onAddonsClick: () -> Unit = {},
    onPluginsClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onSupportersContributorsClick: () -> Unit = {},
    onLicensesAttributionsClick: () -> Unit = {},
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,
    onCollectionsClick: () -> Unit = {},
    onWatchPartyClick: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val screenActive = LocalScreenActive.current
        val pageStateHolder = rememberSaveableStateHolder()

        val selectedTheme by remember {
            ThemeSettingsRepository.ensureLoaded()
            ThemeSettingsRepository.selectedTheme
        }.collectAsStateWithLifecycle()
        val amoledEnabled by remember { ThemeSettingsRepository.amoledEnabled }.collectAsStateWithLifecycle()
        val useNativeNavigation = LocalUseNativeNavigation.current
        val liquidGlassNativeTabBarSupported = remember(useNativeNavigation) {
            !useNativeNavigation && isLiquidGlassNativeTabBarSupported()
        }
        val selectedAppLanguage by remember { ThemeSettingsRepository.selectedAppLanguage }.collectAsStateWithLifecycle()
        val appIconState by remember {
            AppIconRepository.ensureLoaded()
            AppIconRepository.state
        }.collectAsStateWithLifecycle()
        val appIconScope = rememberCoroutineScope()
        val onAppIconSelected: (AppIconOption) -> Unit = { icon ->
            appIconScope.launch { AppIconRepository.select(icon) }
        }
        val tmdbSettings by remember {
            TmdbSettingsRepository.ensureLoaded()
            TmdbSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        val mdbListSettings by remember {
            MdbListSettingsRepository.ensureLoaded()
            MdbListSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        val debridSettings by remember {
            DebridSettingsRepository.ensureLoaded()
            DebridSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        val traktAuthUiState by remember {
            TraktAuthRepository.ensureLoaded()
            TraktAuthRepository.uiState
        }.collectAsStateWithLifecycle()
        val simklAuthUiState by remember {
            SimklAuthRepository.ensureLoaded()
            SimklAuthRepository.uiState
        }.collectAsStateWithLifecycle()
        val traktCommentsEnabled by remember {
            TraktCommentsSettings.ensureLoaded()
            TraktCommentsSettings.enabled
        }.collectAsStateWithLifecycle()
        val mdbListConnected by remember {
            MdbListTracker.ensureLoaded()
            MdbListTracker.isAuthenticated
        }.collectAsStateWithLifecycle()
        val trackingSettingsUiState by remember {
            TrackingSettingsRepository.ensureLoaded()
            TrackingSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        val addonsUiState by remember {
            AddonRepository.initialize()
            AddonRepository.uiState
        }.collectAsStateWithLifecycle()
        val homescreenCatalogRefreshKey = remember(addonsUiState.addons) {
            buildAddonCatalogRefreshSignature(addonsUiState.addons)
        }
        val addonManifestsLoading = addonsUiState.addons.hasPendingEnabledManifests()
        val addonManifestErrorMessage = addonsUiState.addons.firstEnabledManifestError()
        val homescreenSettingsUiState by remember {
            HomeCatalogSettingsRepository.snapshot()
            HomeCatalogSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        val collections by CollectionRepository.collections.collectAsStateWithLifecycle()
        val metaScreenSettingsUiState by remember {
            MetaScreenSettingsRepository.ensureLoaded()
            MetaScreenSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        val continueWatchingPreferencesUiState by remember {
            ContinueWatchingPreferencesRepository.ensureLoaded()
            ContinueWatchingPreferencesRepository.uiState
        }.collectAsStateWithLifecycle()
        val posterCardStyleUiState by remember {
            PosterCardStyleRepository.ensureLoaded()
            PosterCardStyleRepository.uiState
        }.collectAsStateWithLifecycle()
        val episodeReleaseNotificationsUiState by remember {
            EpisodeReleaseNotificationsRepository.ensureLoaded()
            EpisodeReleaseNotificationsRepository.uiState
        }.collectAsStateWithLifecycle()
        val profileSettingsState by remember {
            ProfileRepository.state
        }.collectAsStateWithLifecycle()

        LaunchedEffect(homescreenCatalogRefreshKey) {
            val enabledAddons = addonsUiState.addons.enabledAddons()
            if (!enabledAddons.isWaitingForFirstEnabledManifest()) {
                HomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
            }
        }

        LaunchedEffect(Unit) {
            CollectionRepository.initialize()
        }

        LaunchedEffect(collections) {
            HomeCatalogSettingsRepository.syncCollections(collections)
        }

        val initialPage = remember(initialPageName) {
            runCatching { SettingsPage.valueOf(initialPageName) }
                .getOrDefault(SettingsPage.Root)
                .takeIf { it.isEnabledByPolicy() }
                ?: SettingsPage.Root
        }
        var currentPage by rememberSaveable(initialPageName) { mutableStateOf(initialPage.name) }
        val scrollToTopRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val pageTitles = if (onNavigatePage != null) settingsPageTitles() else emptyMap()
        val page = remember(currentPage) {
            runCatching { SettingsPage.valueOf(currentPage) }
                .getOrDefault(SettingsPage.Root)
                .takeIf { it.isEnabledByPolicy() }
                ?: SettingsPage.Root
        }
        val useDesktopSidebarShell = isDesktop && maxWidth >= 960.dp
        val effectivePage = if (useDesktopSidebarShell) resolveDesktopInitialSettingsPage(page) else page
        val previousPage = if (useDesktopSidebarShell) {
            resolveDesktopParentPage(effectivePage)
        } else {
            page.previousPage()
        }

        fun openPage(targetPage: SettingsPage) {
            if (!targetPage.isEnabledByPolicy()) return
            val externalNavigator = onNavigatePage
            if (externalNavigator == null) {
                currentPage = targetPage.name
                return
            }
            if (targetPage == SettingsPage.Root && onExternalBack != null) {
                onExternalBack()
                return
            }
            externalNavigator(
                targetPage.name,
                pageTitles.getValue(targetPage),
            )
        }

        fun navigateBack() {
            val parentPage = previousPage ?: return
            if (onNavigatePage != null && onExternalBack != null) {
                onExternalBack()
            } else {
                currentPage = parentPage.name
            }
        }

        val openHomescreen = if (onNavigatePage != null) {
            { openPage(SettingsPage.Homescreen) }
        } else {
            onHomescreenClick
        }
        val openMetaScreen = if (onNavigatePage != null) {
            { openPage(SettingsPage.MetaScreen) }
        } else {
            onMetaScreenClick
        }
        val openContinueWatching = if (onNavigatePage != null) {
            { openPage(SettingsPage.ContinueWatching) }
        } else {
            onContinueWatchingClick
        }
        val openAddons = if (onNavigatePage != null) {
            { openPage(SettingsPage.Addons) }
        } else {
            onAddonsClick
        }
        val openPlugins = if (onNavigatePage != null) {
            { openPage(SettingsPage.Plugins) }
        } else {
            onPluginsClick
        }
        val openAccount = if (onNavigatePage != null) {
            { openPage(SettingsPage.Account) }
        } else {
            onAccountClick
        }
        val openSupportersContributors = if (onNavigatePage != null) {
            { openPage(SettingsPage.SupportersContributors) }
        } else {
            onSupportersContributorsClick
        }
        val openLicensesAttributions = if (onNavigatePage != null) {
            { openPage(SettingsPage.LicensesAttributions) }
        } else {
            onLicensesAttributionsClick
        }

        LaunchedEffect(page, currentPage) {
            if (page.name != currentPage) {
                currentPage = page.name
            }
        }

        ScreenActivityEffect(rootActionRequests, rootActionsEnabled, effectivePage, previousPage) { active ->
            if (!active || !rootActionsEnabled) return@ScreenActivityEffect
            rootActionRequests.collect {
                if (previousPage != null) {
                    navigateBack()
                } else {
                    scrollToTopRequests.tryEmit(Unit)
                }
            }
        }

        ScreenActivityEffect(requestedPageName, rootActionsEnabled) { active ->
            if (!active || !rootActionsEnabled) return@ScreenActivityEffect
            val requestedPage = requestedPageName ?: return@ScreenActivityEffect
            val targetPage = runCatching { SettingsPage.valueOf(requestedPage) }.getOrNull()
            if (targetPage == null || !targetPage.isEnabledByPolicy()) {
                onRequestedPageConsumed()
                return@ScreenActivityEffect
            }
            openPage(targetPage)
            onRequestedPageConsumed()
        }

        PlatformBackHandler(
            enabled = screenActive && previousPage != null && (rootActionsEnabled || onExternalBack != null),
            onBack = ::navigateBack,
        )

        if (isSelectedTab || page == SettingsPage.Root) {
            pageStateHolder.SaveableStateProvider("content") {
                if (useDesktopSidebarShell) {
                    DesktopSettingsScreen(
                        page = effectivePage,
                        topChromePadding = topChromePadding,
                        scrollToTopRequests = scrollToTopRequests,
                        onPageChange = ::openPage,
                        onNavigateBack = ::navigateBack,
                        showInternalHeader = showInternalHeader,
                        rememberLastProfileEnabled = profileSettingsState.rememberLastProfileEnabled,
                        selectedTheme = selectedTheme,
                        onThemeSelected = ThemeSettingsRepository::setTheme,
                        amoledEnabled = amoledEnabled,
                        onAmoledToggle = ThemeSettingsRepository::setAmoled,
                        liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                        appIconState = appIconState,
                        onAppIconSelected = onAppIconSelected,
                        onAppIconFailureDismissed = AppIconRepository::clearFailure,
                        selectedAppLanguage = selectedAppLanguage,
                        onAppLanguageSelected = ThemeSettingsRepository::setAppLanguage,
                        episodeReleaseNotificationsUiState = episodeReleaseNotificationsUiState,
                        tmdbSettings = tmdbSettings,
                        mdbListSettings = mdbListSettings,
                        debridSettings = debridSettings,
                        traktAuthUiState = traktAuthUiState,
                        simklAuthUiState = simklAuthUiState,
                        traktCommentsEnabled = traktCommentsEnabled,
                        mdbListConnected = mdbListConnected,
                        trackingSettingsUiState = trackingSettingsUiState,
                        homescreenHeroEnabled = homescreenSettingsUiState.heroEnabled,
                        homescreenShowCatalogType = homescreenSettingsUiState.showCatalogType,
                        homescreenHideUnreleasedContent = homescreenSettingsUiState.hideUnreleasedContent,
                        homescreenItems = homescreenSettingsUiState.items,
                        homescreenCatalogLoading = addonManifestsLoading,
                        homescreenCatalogErrorMessage = addonManifestErrorMessage,
                        metaScreenSettingsUiState = metaScreenSettingsUiState,
                        continueWatchingPreferencesUiState = continueWatchingPreferencesUiState,
                        posterCardStyleUiState = posterCardStyleUiState,
                        onSwitchProfile = onSwitchProfile,
                        onCheckForUpdatesClick = onCheckForUpdatesClick,
                        onTestUpdateBannerClick = onTestUpdateBannerClick,
                        onCollectionsClick = onCollectionsClick,
                        onWatchPartyClick = onWatchPartyClick,
                    )
                } else if (maxWidth >= 960.dp) {
                    TabletSettingsScreen(
                        page = page,
                        topChromePadding = topChromePadding,
                        scrollToTopRequests = scrollToTopRequests,
                        onPageChange = ::openPage,
                        onNavigateBack = ::navigateBack,
                        showInternalHeader = showInternalHeader,
                        rememberLastProfileEnabled = profileSettingsState.rememberLastProfileEnabled,
                        selectedTheme = selectedTheme,
                        onThemeSelected = ThemeSettingsRepository::setTheme,
                        amoledEnabled = amoledEnabled,
                        onAmoledToggle = ThemeSettingsRepository::setAmoled,
                        liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                        appIconState = appIconState,
                        onAppIconSelected = onAppIconSelected,
                        onAppIconFailureDismissed = AppIconRepository::clearFailure,
                        selectedAppLanguage = selectedAppLanguage,
                        onAppLanguageSelected = ThemeSettingsRepository::setAppLanguage,
                        episodeReleaseNotificationsUiState = episodeReleaseNotificationsUiState,
                        tmdbSettings = tmdbSettings,
                        mdbListSettings = mdbListSettings,
                        debridSettings = debridSettings,
                        traktAuthUiState = traktAuthUiState,
                        simklAuthUiState = simklAuthUiState,
                        traktCommentsEnabled = traktCommentsEnabled,
                        mdbListConnected = mdbListConnected,
                        trackingSettingsUiState = trackingSettingsUiState,
                        homescreenHeroEnabled = homescreenSettingsUiState.heroEnabled,
                        homescreenShowCatalogType = homescreenSettingsUiState.showCatalogType,
                        homescreenHideUnreleasedContent = homescreenSettingsUiState.hideUnreleasedContent,
                        homescreenItems = homescreenSettingsUiState.items,
                        homescreenCatalogLoading = addonManifestsLoading,
                        homescreenCatalogErrorMessage = addonManifestErrorMessage,
                        metaScreenSettingsUiState = metaScreenSettingsUiState,
                        continueWatchingPreferencesUiState = continueWatchingPreferencesUiState,
                        posterCardStyleUiState = posterCardStyleUiState,
                        onSwitchProfile = onSwitchProfile,
                        onSupportersContributorsClick = openSupportersContributors,
                        onLicensesAttributionsClick = openLicensesAttributions,
                        onCheckForUpdatesClick = onCheckForUpdatesClick,
                        onTestUpdateBannerClick = onTestUpdateBannerClick,
                        onCollectionsClick = onCollectionsClick,
                        onWatchPartyClick = onWatchPartyClick,
                    )
                } else {
                    MobileSettingsScreen(
                        page = page,
                        isTabletLayout = maxWidth >= 768.dp,
                        topChromePadding = topChromePadding,
                        scrollToTopRequests = scrollToTopRequests,
                        onPageChange = ::openPage,
                        onNavigateBack = ::navigateBack,
                        showInternalHeader = showInternalHeader,
                        rememberLastProfileEnabled = profileSettingsState.rememberLastProfileEnabled,
                        selectedTheme = selectedTheme,
                        onThemeSelected = ThemeSettingsRepository::setTheme,
                        amoledEnabled = amoledEnabled,
                        onAmoledToggle = ThemeSettingsRepository::setAmoled,
                        liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                        appIconState = appIconState,
                        onAppIconSelected = onAppIconSelected,
                        onAppIconFailureDismissed = AppIconRepository::clearFailure,
                        selectedAppLanguage = selectedAppLanguage,
                        onAppLanguageSelected = ThemeSettingsRepository::setAppLanguage,
                        episodeReleaseNotificationsUiState = episodeReleaseNotificationsUiState,
                        tmdbSettings = tmdbSettings,
                        mdbListSettings = mdbListSettings,
                        debridSettings = debridSettings,
                        traktAuthUiState = traktAuthUiState,
                        simklAuthUiState = simklAuthUiState,
                        traktCommentsEnabled = traktCommentsEnabled,
                        mdbListConnected = mdbListConnected,
                        trackingSettingsUiState = trackingSettingsUiState,
                        homescreenHeroEnabled = homescreenSettingsUiState.heroEnabled,
                        homescreenShowCatalogType = homescreenSettingsUiState.showCatalogType,
                        homescreenHideUnreleasedContent = homescreenSettingsUiState.hideUnreleasedContent,
                        homescreenItems = homescreenSettingsUiState.items,
                        homescreenCatalogLoading = addonManifestsLoading,
                        homescreenCatalogErrorMessage = addonManifestErrorMessage,
                        metaScreenSettingsUiState = metaScreenSettingsUiState,
                        continueWatchingPreferencesUiState = continueWatchingPreferencesUiState,
                        posterCardStyleUiState = posterCardStyleUiState,
                        onSwitchProfile = onSwitchProfile,
                        onHomescreenClick = openHomescreen,
                        onMetaScreenClick = openMetaScreen,
                        onContinueWatchingClick = openContinueWatching,
                        onAddonsClick = openAddons,
                        onPluginsClick = openPlugins,
                        onAccountClick = openAccount,
                        onSupportersContributorsClick = openSupportersContributors,
                        onLicensesAttributionsClick = openLicensesAttributions,
                        onCheckForUpdatesClick = onCheckForUpdatesClick,
                        onTestUpdateBannerClick = onTestUpdateBannerClick,
                        onCollectionsClick = onCollectionsClick,
                        onWatchPartyClick = onWatchPartyClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileSettingsScreen(
    page: SettingsPage,
    isTabletLayout: Boolean = false,
    topChromePadding: Dp? = null,
    scrollToTopRequests: Flow<Unit>,
    onPageChange: (SettingsPage) -> Unit,
    onNavigateBack: () -> Unit,
    showInternalHeader: Boolean,
    rememberLastProfileEnabled: Boolean,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    amoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    liquidGlassNativeTabBarSupported: Boolean,
    appIconState: AppIconSettingsState,
    onAppIconSelected: (AppIconOption) -> Unit,
    onAppIconFailureDismissed: () -> Unit,
    selectedAppLanguage: AppLanguage,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    episodeReleaseNotificationsUiState: EpisodeReleaseNotificationsUiState,
    tmdbSettings: TmdbSettings,
    mdbListSettings: MdbListSettings,
    debridSettings: DebridSettings,
    traktAuthUiState: TraktAuthUiState,
    simklAuthUiState: SimklAuthUiState,
    traktCommentsEnabled: Boolean,
    mdbListConnected: Boolean,
    trackingSettingsUiState: TrackingSettingsUiState,
    homescreenHeroEnabled: Boolean,
    homescreenShowCatalogType: Boolean,
    homescreenHideUnreleasedContent: Boolean,
    homescreenItems: List<HomeCatalogSettingsItem>,
    homescreenCatalogLoading: Boolean,
    homescreenCatalogErrorMessage: String?,
    metaScreenSettingsUiState: MetaScreenSettingsUiState,
    continueWatchingPreferencesUiState: ContinueWatchingPreferencesUiState,
    posterCardStyleUiState: PosterCardStyleUiState,
    onSwitchProfile: (() -> Unit)? = null,
    onHomescreenClick: () -> Unit = {},
    onMetaScreenClick: () -> Unit = {},
    onContinueWatchingClick: () -> Unit = {},
    onAddonsClick: () -> Unit = {},
    onPluginsClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onSupportersContributorsClick: () -> Unit = {},
    onLicensesAttributionsClick: () -> Unit = {},
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,
    onCollectionsClick: () -> Unit = {},
    onWatchPartyClick: () -> Unit = {},
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    saveableStateHolder.SaveableStateProvider(page.name) {
        var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
        var rootSearchVisible by rememberSaveable { mutableStateOf(isDesktop) }
        var rootSearchRevealAnimating by rememberSaveable { mutableStateOf(false) }
        val listState = rememberLazyListState()
        ScreenActivityEffect(listState) { screenActive ->
            if (!screenActive) listState.stopScroll()
        }
        val hapticFeedback = LocalHapticFeedback.current
        val hapticScope = rememberCoroutineScope()
        val rootSearchRevealConnection = rememberSettingsRootSearchRevealConnection(
            page = page,
            listState = listState,
            query = settingsSearchQuery,
            searchVisible = rootSearchVisible,
        ) {
            rootSearchVisible = true
            rootSearchRevealAnimating = true
            hapticScope.launch {
                delay(SettingsSearchRevealHapticDelayMillis)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        fun openSearchTarget(target: SettingsSearchTarget) {
            when (target) {
                is SettingsSearchTarget.Page -> when (target.page) {
                    SettingsPage.Account -> onAccountClick()
                    SettingsPage.SupportersContributors -> {
                        if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                            onSupportersContributorsClick()
                        }
                    }
                    SettingsPage.LicensesAttributions -> onLicensesAttributionsClick()
                    SettingsPage.ContinueWatching -> onContinueWatchingClick()
                    SettingsPage.Addons -> onAddonsClick()
                    SettingsPage.Plugins -> {
                        if (AppFeaturePolicy.pluginsEnabled) {
                            onPluginsClick()
                        }
                    }
                    SettingsPage.Homescreen -> onHomescreenClick()
                    SettingsPage.MetaScreen -> onMetaScreenClick()
                    else -> onPageChange(target.page)
                }
                SettingsSearchTarget.Collections -> onCollectionsClick()
                SettingsSearchTarget.SwitchProfile -> onSwitchProfile?.invoke()
                SettingsSearchTarget.CheckForUpdates -> onCheckForUpdatesClick?.invoke()
            }
        }

        LaunchedEffect(rootSearchRevealAnimating) {
            if (rootSearchRevealAnimating) {
                delay(SettingsSearchRevealAnimationMillis)
                rootSearchRevealAnimating = false
            }
        }

        LaunchedEffect(scrollToTopRequests) {
            scrollToTopRequests.collect {
                listState.animateScrollToItem(0)
            }
        }

        val watchPartyRowClick = watchPartyRow(onWatchPartyClick)
        NuvioScreen(
            modifier = Modifier.nestedScroll(rootSearchRevealConnection),
            topPadding = if (topChromePadding != null) 0.dp else null,
            listState = listState,
        ) {
            if (showInternalHeader) {
                stickyHeader {
                    val previousPage = page.previousPage()
                    NuvioScreenHeader(
                        title = stringResource(page.titleRes),
                        topPadding = topChromePadding,
                        onBack = previousPage?.let { { onNavigateBack() } },
                    )
                }
            } else {
                item { Spacer(modifier = Modifier.height(44.dp)) }
            }

            when (page) {
                SettingsPage.Root -> {
                    settingsSearchRootContent(
                        query = settingsSearchQuery,
                        entries = {
                            settingsSearchEntries(
                                isTablet = false,
                                pluginsEnabled = AppFeaturePolicy.pluginsEnabled,
                                notificationsEnabled = AppFeaturePolicy.notificationsEnabled,
                                externalPlayerSupported = AppFeaturePolicy.externalPlayerSupported,
                                supportersContributorsPageEnabled = AppFeaturePolicy.supportersContributorsPageEnabled,
                                accountDeletionEnabled = AppFeaturePolicy.accountDeletionEnabled,
                                personalMediaAddonCopyEnabled = AppFeaturePolicy.personalMediaAddonCopyEnabled,
                                liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                                switchProfileAvailable = onSwitchProfile != null,
                                checkForUpdatesAvailable = onCheckForUpdatesClick != null,
                            )
                        },
                        isTablet = false,
                        showSearchField = rootSearchVisible,
                        animateSearchField = rootSearchRevealAnimating,
                        onQueryChange = { settingsSearchQuery = it },
                        onTargetClick = { openSearchTarget(it) },
                    )
                    if (settingsSearchQuery.isBlank()) {
                        settingsRootContent(
                            isTablet = false,
                            onPageClick = { openSearchTarget(SettingsSearchTarget.Page(it)) },
                            onSwitchProfileClick = onSwitchProfile,
                            onWatchPartyClick = watchPartyRowClick,
                            showNotificationsEntry = AppFeaturePolicy.notificationsEnabled,
                        )
                    }
                }
                SettingsPage.Account -> accountSettingsContent(
                    isTablet = false,
                )
                SettingsPage.SupportersContributors -> {
                    if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                        supportersContributorsContent(isTablet = false)
                    }
                }
                SettingsPage.LicensesAttributions -> licensesAttributionsContent(
                    isTablet = false,
                )
                SettingsPage.Playback,
                SettingsPage.PlaybackPlayer,
                SettingsPage.PlaybackStreamSelection,
                SettingsPage.PlaybackUpNext,
                SettingsPage.PlaybackSkipSegments,
                SettingsPage.PlaybackInterface,
                SettingsPage.PlaybackAudio,
                SettingsPage.PlaybackSubtitles,
                SettingsPage.PlaybackVideo,
                SettingsPage.PlaybackBuffer,
                SettingsPage.PlaybackP2p -> playbackSettingsContent(
                    isTablet = false,
                    page = page,
                    onPageChange = onPageChange,
                )
                SettingsPage.Streams -> streamsSettingsContent(
                    isTablet = false,
                )
                SettingsPage.Appearance -> appearanceSettingsContent(
                    isTablet = isTabletLayout,
                    selectedTheme = selectedTheme,
                    onThemeSelected = onThemeSelected,
                    amoledEnabled = amoledEnabled,
                    onAmoledToggle = onAmoledToggle,
                    appIconState = appIconState,
                    onAppIconSelected = onAppIconSelected,
                    onAppIconFailureDismissed = onAppIconFailureDismissed,
                    selectedAppLanguage = selectedAppLanguage,
                    onAppLanguageSelected = onAppLanguageSelected,
                )
                SettingsPage.Layout -> layoutSettingsContent(
                    isTablet = isTabletLayout,
                    liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                    onHomescreenClick = onHomescreenClick,
                    onMetaScreenClick = onMetaScreenClick,
                    onStreamsClick = { onPageChange(SettingsPage.Streams) },
                    onCollectionsClick = onCollectionsClick,
                    onContinueWatchingClick = onContinueWatchingClick,
                    onPosterCustomizationClick = { onPageChange(SettingsPage.PosterCustomization) },
                    onHoverPreviewClick = { onPageChange(SettingsPage.HoverPreview) },
                )
                SettingsPage.HoverPreview -> hoverPreviewSettingsContent(
                    isTablet = isTabletLayout,
                    uiState = posterCardStyleUiState,
                )
                SettingsPage.About -> aboutSettingsContent(
                    isTablet = false,
                    onSupportersContributorsClick = onSupportersContributorsClick,
                    onLicensesAttributionsClick = onLicensesAttributionsClick,
                    onCheckForUpdatesClick = onCheckForUpdatesClick,
                    onTestUpdateBannerClick = onTestUpdateBannerClick,
                    showSupportersContributorsPage = AppFeaturePolicy.supportersContributorsPageEnabled,
                )
                SettingsPage.Advanced -> advancedSettingsContent(
                    isTablet = false,
                    rememberLastProfileEnabled = rememberLastProfileEnabled,
                )
                SettingsPage.Notifications -> notificationsSettingsContent(
                    isTablet = false,
                    uiState = episodeReleaseNotificationsUiState,
                )
                SettingsPage.ContinueWatching -> continueWatchingSettingsContent(
                    isTablet = false,
                    isVisible = continueWatchingPreferencesUiState.isVisible,
                    style = continueWatchingPreferencesUiState.style,
                    upNextFromFurthestEpisode = continueWatchingPreferencesUiState.upNextFromFurthestEpisode,
                    useEpisodeThumbnails = continueWatchingPreferencesUiState.useEpisodeThumbnails,
                    showUnairedNextUp = continueWatchingPreferencesUiState.showUnairedNextUp,
                    blurNextUp = continueWatchingPreferencesUiState.blurNextUp,
                    showResumePromptOnLaunch = continueWatchingPreferencesUiState.showResumePromptOnLaunch,
                    sortMode = continueWatchingPreferencesUiState.sortMode,
                    clickAction = continueWatchingPreferencesUiState.clickAction,
                )
                SettingsPage.PosterCustomization -> posterCustomizationSettingsContent(
                    isTablet = false,
                    uiState = posterCardStyleUiState,
                )
                SettingsPage.ContentDiscovery -> contentDiscoveryContent(
                    isTablet = false,
                    showPluginsEntry = AppFeaturePolicy.pluginsEnabled,
                    onAddonsClick = onAddonsClick,
                    onPluginsClick = onPluginsClick,
                    onMediaServersClick = { onPageChange(SettingsPage.MediaServers) },
                )
                SettingsPage.Addons -> addonsSettingsContent()
                SettingsPage.MediaServers -> mediaServersSettingsContent(
                    isTablet = false,
                    onServerClick = { onPageChange(SettingsPage.MediaServer) },
                )
                SettingsPage.MediaServer -> mediaServerSettingsContent(
                    isTablet = false,
                    onBack = onNavigateBack,
                )
                SettingsPage.Plugins -> if (AppFeaturePolicy.pluginsEnabled) pluginsSettingsContent() else addonsSettingsContent()
                SettingsPage.Homescreen -> homescreenSettingsContent(
                    isTablet = false,
                    heroEnabled = homescreenHeroEnabled,
                    showCatalogType = homescreenShowCatalogType,
                    hideUnreleasedContent = homescreenHideUnreleasedContent,
                    items = homescreenItems,
                    isCatalogLoading = homescreenCatalogLoading,
                    catalogErrorMessage = homescreenCatalogErrorMessage,
                )
                SettingsPage.MetaScreen -> metaScreenSettingsContent(
                    isTablet = false,
                    uiState = metaScreenSettingsUiState,
                )
                SettingsPage.Integrations -> integrationsContent(
                    isTablet = false,
                    onTmdbClick = { onPageChange(SettingsPage.TmdbEnrichment) },
                    onMdbListClick = { onPageChange(SettingsPage.MdbListRatings) },
                    onDebridClick = { onPageChange(SettingsPage.Debrid) },
                )
                SettingsPage.TmdbEnrichment -> tmdbSettingsContent(
                    isTablet = false,
                    settings = tmdbSettings,
                )
                SettingsPage.MdbListRatings -> mdbListSettingsContent(
                    isTablet = false,
                    settings = mdbListSettings,
                )
                SettingsPage.Debrid -> debridSettingsContent(
                    isTablet = false,
                    settings = debridSettings,
                )
                SettingsPage.TraktAuthentication -> trackingSettingsContent(
                    isTablet = false,
                    traktUiState = traktAuthUiState,
                    simklUiState = simklAuthUiState,
                    settingsUiState = trackingSettingsUiState,
                    commentsEnabled = traktCommentsEnabled,
                    mdbListConnected = mdbListConnected,
                    onCommentsEnabledChange = TraktCommentsSettings::setEnabled,
                )
            }
        }
    }
}

@Composable
private fun rememberSettingsRootSearchRevealConnection(
    page: SettingsPage,
    listState: LazyListState,
    query: String,
    searchVisible: Boolean,
    onReveal: () -> Unit,
): NestedScrollConnection {
    val revealThresholdPx = with(LocalDensity.current) { SettingsSearchRevealThreshold.toPx() }
    val currentOnReveal by rememberUpdatedState(onReveal)
    var pullDistancePx by remember(page) { mutableStateOf(0f) }
    var revealTriggered by remember(page) { mutableStateOf(false) }

    return remember(page, listState, query, searchVisible, revealThresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val isRootAtTop = page == SettingsPage.Root &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                val canRevealSearch = isRootAtTop && !searchVisible && !revealTriggered && query.isBlank()

                if (canRevealSearch && available.y > 0f) {
                    pullDistancePx += available.y
                    if (pullDistancePx >= revealThresholdPx) {
                        pullDistancePx = 0f
                        revealTriggered = true
                        currentOnReveal()
                    }
                } else if (!isRootAtTop || available.y < 0f) {
                    pullDistancePx = 0f
                }

                return Offset.Zero
            }
        }
    }
}

@Composable
private fun TabletSettingsScreen(
    page: SettingsPage,
    topChromePadding: Dp? = null,
    scrollToTopRequests: Flow<Unit>,
    onPageChange: (SettingsPage) -> Unit,
    onNavigateBack: () -> Unit,
    showInternalHeader: Boolean,
    rememberLastProfileEnabled: Boolean,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    amoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    liquidGlassNativeTabBarSupported: Boolean,
    appIconState: AppIconSettingsState,
    onAppIconSelected: (AppIconOption) -> Unit,
    onAppIconFailureDismissed: () -> Unit,
    selectedAppLanguage: AppLanguage,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    episodeReleaseNotificationsUiState: EpisodeReleaseNotificationsUiState,
    tmdbSettings: TmdbSettings,
    mdbListSettings: MdbListSettings,
    debridSettings: DebridSettings,
    traktAuthUiState: TraktAuthUiState,
    simklAuthUiState: SimklAuthUiState,
    traktCommentsEnabled: Boolean,
    mdbListConnected: Boolean,
    trackingSettingsUiState: TrackingSettingsUiState,
    homescreenHeroEnabled: Boolean,
    homescreenShowCatalogType: Boolean,
    homescreenHideUnreleasedContent: Boolean,
    homescreenItems: List<HomeCatalogSettingsItem>,
    homescreenCatalogLoading: Boolean,
    homescreenCatalogErrorMessage: String?,
    metaScreenSettingsUiState: MetaScreenSettingsUiState,
    continueWatchingPreferencesUiState: ContinueWatchingPreferencesUiState,
    posterCardStyleUiState: PosterCardStyleUiState,
    onSwitchProfile: (() -> Unit)? = null,
    onSupportersContributorsClick: () -> Unit = {},
    onLicensesAttributionsClick: () -> Unit = {},
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,
    onCollectionsClick: () -> Unit = {},
    onWatchPartyClick: () -> Unit = {},
) {
    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.Look.name) }
    val activeCategory = settingsCategoryForName(selectedCategory)
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topOffset = topChromePadding ?: (max(statusBarPadding + 24.dp, 48.dp) + 64.dp)

    LaunchedEffect(page) {
        if (page.opensInlineOnTablet) {
            selectedCategory = page.category.name
        }
    }

    fun openInlinePage(page: SettingsPage) {
        selectedCategory = page.category.name
        onPageChange(page)
    }

    val tokens = MaterialTheme.nuvio
    val saveableStateHolder = rememberSaveableStateHolder()
    val categoryEntryRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background),
    ) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight(),
            color = tokens.colors.surfaceElevated,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topOffset),
            ) {
                Text(
                    text = stringResource(Res.string.compose_settings_page_root),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NuvioTokens.Space.s24)
                        .padding(bottom = NuvioTokens.Space.s20),
                    style = MaterialTheme.typography.displayLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(
                    thickness = tokens.borders.hairline,
                    color = tokens.colors.borderSubtle,
                )

                Spacer(modifier = Modifier.height(NuvioTokens.Space.s12))
                SettingsCategory.entries.forEach { category ->
                    SettingsSidebarItem(
                        label = stringResource(category.labelRes),
                        icon = category.icon,
                        selected = category == activeCategory,
                        onClick = {
                            selectedCategory = category.name
                            if (page != SettingsPage.Root) {
                                onPageChange(SettingsPage.Root)
                            }
                        },
                        modifier = Modifier
                            .focusProperties {
                                if (category == SettingsCategory.entries.first()) {
                                    up = FocusRequester.Cancel
                                }
                                if (category == SettingsCategory.entries.last()) {
                                    down = FocusRequester.Cancel
                                }
                                right = ContentEntry.requester
                            }
                            .then(
                                if (category == activeCategory) {
                                    Modifier.focusRequester(categoryEntryRequester)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(tokens.borders.thin)
                .fillMaxHeight()
                .background(tokens.colors.borderSubtle),
        )

        saveableStateHolder.SaveableStateProvider(page.name) {
            var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
            var rootSearchVisible by rememberSaveable { mutableStateOf(isDesktop) }
            var rootSearchRevealAnimating by rememberSaveable { mutableStateOf(false) }
            val hapticFeedback = LocalHapticFeedback.current
            val hapticScope = rememberCoroutineScope()
            fun openSearchTarget(target: SettingsSearchTarget) {
                when (target) {
                    is SettingsSearchTarget.Page -> {
                        if (target.page.isEnabledByPolicy()) {
                            openInlinePage(target.page)
                        }
                    }
                    SettingsSearchTarget.Collections -> onCollectionsClick()
                    SettingsSearchTarget.SwitchProfile -> onSwitchProfile?.invoke()
                    SettingsSearchTarget.CheckForUpdates -> onCheckForUpdatesClick?.invoke()
                }
            }

            val listState = rememberLazyListState()
            ScreenActivityEffect(listState) { screenActive ->
                if (!screenActive) listState.stopScroll()
            }
            val bottomOverlayPadding = LocalNuvioBottomNavigationOverlayPadding.current
            val rootSearchRevealConnection = rememberSettingsRootSearchRevealConnection(
                page = page,
                listState = listState,
                query = settingsSearchQuery,
                searchVisible = rootSearchVisible,
            ) {
                rootSearchVisible = true
                rootSearchRevealAnimating = true
                hapticScope.launch {
                    delay(SettingsSearchRevealHapticDelayMillis)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            LaunchedEffect(rootSearchRevealAnimating) {
                if (rootSearchRevealAnimating) {
                    delay(SettingsSearchRevealAnimationMillis)
                    rootSearchRevealAnimating = false
                }
            }
            LaunchedEffect(scrollToTopRequests) {
                scrollToTopRequests.collect {
                    listState.animateScrollToItem(0)
                }
            }
            val watchPartyRowClick = watchPartyRow(onWatchPartyClick)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(tokens.colors.background)
                    .nuvioContentEntry(),
            ) {
                if (isDesktop) {
                    val ambientAlpha = if (tokens.colors.background == Color.Black) 0.03f else 0.055f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        tokens.colors.accent.copy(alpha = ambientAlpha),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                }
                val baseHorizontalPadding = NuvioTokens.Space.s40
                val maxContentWidth = 820.dp
                val contentHorizontalPadding = if (maxWidth > maxContentWidth + baseHorizontalPadding * 2) {
                    (maxWidth - maxContentWidth) / 2
                } else {
                    baseHorizontalPadding
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(rootSearchRevealConnection),
                    contentPadding = PaddingValues(
                        start = contentHorizontalPadding,
                        top = topOffset,
                        end = contentHorizontalPadding,
                        bottom = NuvioTokens.Space.s40 + bottomOverlayPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s18),
                ) {
                    if (showInternalHeader) {
                        item {
                            val previousPage = page.previousPage()
                            TabletPageHeader(
                                title = if (page == SettingsPage.Root) {
                                    if (settingsSearchQuery.isBlank()) {
                                        stringResource(activeCategory.labelRes)
                                    } else {
                                        stringResource(Res.string.compose_settings_page_root)
                                    }
                                } else {
                                    stringResource(page.titleRes)
                                },
                                showBack = previousPage != null,
                                onBack = onNavigateBack,
                            )
                        }
                    }
                    when (page) {
                        SettingsPage.Root -> {
                            settingsSearchRootContent(
                                query = settingsSearchQuery,
                                entries = {
                                    settingsSearchEntries(
                                        isTablet = true,
                                        pluginsEnabled = AppFeaturePolicy.pluginsEnabled,
                                        notificationsEnabled = AppFeaturePolicy.notificationsEnabled,
                                        externalPlayerSupported = AppFeaturePolicy.externalPlayerSupported,
                                        supportersContributorsPageEnabled = AppFeaturePolicy.supportersContributorsPageEnabled,
                                        accountDeletionEnabled = AppFeaturePolicy.accountDeletionEnabled,
                                        personalMediaAddonCopyEnabled = AppFeaturePolicy.personalMediaAddonCopyEnabled,
                                        liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                                        switchProfileAvailable = onSwitchProfile != null,
                                        checkForUpdatesAvailable = onCheckForUpdatesClick != null,
                                    )
                                },
                                isTablet = true,
                                showSearchField = rootSearchVisible,
                                animateSearchField = rootSearchRevealAnimating,
                                onQueryChange = { settingsSearchQuery = it },
                                onTargetClick = { openSearchTarget(it) },
                            )
                            if (settingsSearchQuery.isBlank()) {
                                settingsRootContent(
                                    isTablet = true,
                                    onPageClick = ::openInlinePage,
                                    onSwitchProfileClick = onSwitchProfile,
                                    onWatchPartyClick = watchPartyRowClick,
                                    showNotificationsEntry = AppFeaturePolicy.notificationsEnabled,
                                    category = activeCategory,
                                )
                            }
                        }
                        else -> settingsDetailPageContent(
                            page = page,
                            onPageChange = ::openInlinePage,
                            onNavigateBack = onNavigateBack,
                            rememberLastProfileEnabled = rememberLastProfileEnabled,
                            selectedTheme = selectedTheme,
                            onThemeSelected = onThemeSelected,
                            amoledEnabled = amoledEnabled,
                            onAmoledToggle = onAmoledToggle,
                            liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                            appIconState = appIconState,
                            onAppIconSelected = onAppIconSelected,
                            onAppIconFailureDismissed = onAppIconFailureDismissed,
                            selectedAppLanguage = selectedAppLanguage,
                            onAppLanguageSelected = onAppLanguageSelected,
                            episodeReleaseNotificationsUiState = episodeReleaseNotificationsUiState,
                            tmdbSettings = tmdbSettings,
                            mdbListSettings = mdbListSettings,
                            debridSettings = debridSettings,
                            traktAuthUiState = traktAuthUiState,
                            simklAuthUiState = simklAuthUiState,
                            traktCommentsEnabled = traktCommentsEnabled,
                            mdbListConnected = mdbListConnected,
                            trackingSettingsUiState = trackingSettingsUiState,
                            homescreenHeroEnabled = homescreenHeroEnabled,
                            homescreenShowCatalogType = homescreenShowCatalogType,
                            homescreenHideUnreleasedContent = homescreenHideUnreleasedContent,
                            homescreenItems = homescreenItems,
                            homescreenCatalogLoading = homescreenCatalogLoading,
                            homescreenCatalogErrorMessage = homescreenCatalogErrorMessage,
                            metaScreenSettingsUiState = metaScreenSettingsUiState,
                            continueWatchingPreferencesUiState = continueWatchingPreferencesUiState,
                            posterCardStyleUiState = posterCardStyleUiState,
                            onCheckForUpdatesClick = onCheckForUpdatesClick,
                            onTestUpdateBannerClick = onTestUpdateBannerClick,
                            onCollectionsClick = onCollectionsClick,
                        )
                    }
                }
                if (isDesktop) {
                    NuvioDesktopVerticalScrollbar(
                        state = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(top = topOffset, bottom = 8.dp, end = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopSettingsScreen(
    page: SettingsPage,
    topChromePadding: Dp? = null,
    scrollToTopRequests: Flow<Unit>,
    onPageChange: (SettingsPage) -> Unit,
    onNavigateBack: () -> Unit,
    showInternalHeader: Boolean,
    rememberLastProfileEnabled: Boolean,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    amoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    liquidGlassNativeTabBarSupported: Boolean,
    appIconState: AppIconSettingsState,
    onAppIconSelected: (AppIconOption) -> Unit,
    onAppIconFailureDismissed: () -> Unit,
    selectedAppLanguage: AppLanguage,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    episodeReleaseNotificationsUiState: EpisodeReleaseNotificationsUiState,
    tmdbSettings: TmdbSettings,
    mdbListSettings: MdbListSettings,
    debridSettings: DebridSettings,
    traktAuthUiState: TraktAuthUiState,
    simklAuthUiState: SimklAuthUiState,
    traktCommentsEnabled: Boolean,
    mdbListConnected: Boolean,
    trackingSettingsUiState: TrackingSettingsUiState,
    homescreenHeroEnabled: Boolean,
    homescreenShowCatalogType: Boolean,
    homescreenHideUnreleasedContent: Boolean,
    homescreenItems: List<HomeCatalogSettingsItem>,
    homescreenCatalogLoading: Boolean,
    homescreenCatalogErrorMessage: String?,
    metaScreenSettingsUiState: MetaScreenSettingsUiState,
    continueWatchingPreferencesUiState: ContinueWatchingPreferencesUiState,
    posterCardStyleUiState: PosterCardStyleUiState,
    onSwitchProfile: (() -> Unit)? = null,
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,
    onCollectionsClick: () -> Unit = {},
    onWatchPartyClick: () -> Unit = {},
) {
    val tokens = MaterialTheme.nuvio
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val desktopSidebarTopOffset = if (topChromePadding != null) {
        max(topChromePadding - 28.dp, NuvioTokens.Space.s24)
    } else {
        max(statusBarPadding + NuvioTokens.Space.s16, NuvioTokens.Space.s24)
    }
    val detailTopOffset = if (topChromePadding != null) {
        max(topChromePadding - 16.dp, NuvioTokens.Space.s28)
    } else {
        NuvioTokens.Space.s28
    }

    val route = resolveDesktopWorkspaceRoute(page)
    val selectedSidebarPage = route.workspace.primaryPage
    val parentPage = resolveDesktopParentPage(page)
    var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
    var systemTabOverride by remember { mutableStateOf<DesktopSettingsWorkspaceTab?>(null) }
    val watchPartyRowClick = watchPartyRow(onWatchPartyClick)
    val navSections = remember { buildDesktopSettingsNavSections() }
    val allSidebarItems = remember(navSections) { navSections.flatMap { it.items } }
    val firstSidebarItemId = allSidebarItems.firstOrNull()?.id
    val lastSidebarItemId = allSidebarItems.lastOrNull()?.id
    val sidebarEntryRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val sidebarListState = rememberLazyListState()
    val saveableStateHolder = rememberSaveableStateHolder()
    val bottomOverlayPadding = LocalNuvioBottomNavigationOverlayPadding.current

    val workspaceTabs = remember(
        route.workspace,
        AppFeaturePolicy.pluginsEnabled,
        AppFeaturePolicy.notificationsEnabled,
        SentrySettingsRepository.isSupported,
    ) {
        desktopWorkspaceTabs(
            workspace = route.workspace,
            showPlugins = AppFeaturePolicy.pluginsEnabled,
            showNotifications = AppFeaturePolicy.notificationsEnabled,
            showSentry = SentrySettingsRepository.isSupported,
        )
    }
    val activeTab = if (
        route.workspace == DesktopSettingsWorkspace.SystemAndPerformance &&
        page == SettingsPage.Advanced &&
        systemTabOverride != null &&
        systemTabOverride in workspaceTabs
    ) {
        systemTabOverride
    } else {
        route.selectedTab?.takeIf { it in workspaceTabs } ?: workspaceTabs.firstOrNull()
    }

    LaunchedEffect(page) {
        settingsSearchQuery = ""
        if (route.workspace != DesktopSettingsWorkspace.SystemAndPerformance || page != SettingsPage.Advanced) {
            systemTabOverride = null
        }
    }

    fun handleSearchTarget(target: SettingsSearchTarget) {
        settingsSearchQuery = ""
        systemTabOverride = null
        when (target) {
            is SettingsSearchTarget.Page -> {
                if (target.page.isEnabledByPolicy()) {
                    onPageChange(target.page)
                }
            }
            SettingsSearchTarget.Collections -> onCollectionsClick()
            SettingsSearchTarget.SwitchProfile -> onSwitchProfile?.invoke()
            SettingsSearchTarget.CheckForUpdates -> onCheckForUpdatesClick?.invoke()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.key == Key.K &&
                    (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)
                ) {
                    searchFocusRequester.requestFocus()
                    true
                } else {
                    false
                }
            },
    ) {
        Surface(
            modifier = Modifier
                .width(264.dp)
                .fillMaxHeight(),
            color = tokens.colors.surfaceElevated,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = desktopSidebarTopOffset,
                            start = NuvioTokens.Space.s14,
                            end = NuvioTokens.Space.s14,
                            bottom = NuvioTokens.Space.s10,
                        ),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
                ) {
                    Text(
                        text = stringResource(Res.string.compose_settings_page_root),
                        style = MaterialTheme.nuvioTypeScale.titleMd.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = tokens.colors.textPrimary,
                    )
                    DesktopSettingsSidebarSearchBar(
                        query = settingsSearchQuery,
                        onQueryChange = { settingsSearchQuery = it },
                        focusRequester = searchFocusRequester,
                    )
                }

                HorizontalDivider(
                    thickness = tokens.borders.hairline,
                    color = tokens.colors.borderSubtle,
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    LazyColumn(
                        state = sidebarListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .desktopSmoothWheelScroll(sidebarListState),
                        contentPadding = PaddingValues(
                            start = NuvioTokens.Space.s10,
                            end = NuvioTokens.Space.s10,
                            top = NuvioTokens.Space.s2,
                            bottom = NuvioTokens.Space.s10,
                        ),
                        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s2),
                    ) {
                        navSections.forEach { section ->
                            item(key = "header-${section.group.name}") {
                                DesktopSettingsSidebarSectionHeader(
                                    title = stringResource(section.group.labelRes),
                                )
                            }
                            items(
                                items = section.items,
                                key = { it.id },
                            ) { item ->
                                val isSelected = settingsSearchQuery.isBlank() &&
                                    item.destination == DesktopSettingsDestination.Page(selectedSidebarPage)
                                val onItemClick: () -> Unit = {
                                    settingsSearchQuery = ""
                                    systemTabOverride = null
                                    when (val dest = item.destination) {
                                        is DesktopSettingsDestination.Page -> onPageChange(dest.page)
                                        DesktopSettingsDestination.SwitchProfile -> onSwitchProfile?.invoke()
                                        DesktopSettingsDestination.WatchParty -> watchPartyRowClick?.invoke()
                                        DesktopSettingsDestination.Collections -> onCollectionsClick()
                                    }
                                }
                                DesktopSettingsSidebarItem(
                                    label = stringResource(item.titleRes),
                                    icon = item.icon,
                                    selected = isSelected,
                                    onClick = onItemClick,
                                    modifier = Modifier
                                        .focusProperties {
                                            if (item.id == firstSidebarItemId) {
                                                up = FocusRequester.Cancel
                                            }
                                            if (item.id == lastSidebarItemId) {
                                                down = FocusRequester.Cancel
                                            }
                                            end = ContentEntry.requester
                                        }
                                        .then(
                                            if (isSelected) {
                                                Modifier.focusRequester(sidebarEntryRequester)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                )
                            }
                        }
                    }

                    NuvioDesktopVerticalScrollbar(
                        state = sidebarListState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = NuvioTokens.Space.s6, horizontal = NuvioTokens.Space.s2),
                    )
                }

                HorizontalDivider(
                    thickness = tokens.borders.hairline,
                    color = tokens.colors.borderSubtle,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s10),
                ) {
                    DesktopSettingsSidebarVersionCard(
                        onCheckForUpdatesClick = onCheckForUpdatesClick,
                        onOpenAboutClick = {
                            settingsSearchQuery = ""
                            systemTabOverride = null
                            onPageChange(SettingsPage.About)
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(tokens.borders.thin)
                .fillMaxHeight()
                .background(tokens.colors.borderSubtle),
        )

        val isSearchActive = settingsSearchQuery.isNotBlank()
        val detailStateKey = when {
            isSearchActive -> "desktop-search"
            route.subDetailPage != null -> route.subDetailPage.name
            activeTab != null -> "${route.workspace.name}:${activeTab.id}"
            else -> route.workspace.name
        }
        saveableStateHolder.SaveableStateProvider(detailStateKey) {
            val detailListState = rememberLazyListState()
            ScreenActivityEffect(detailListState) { screenActive ->
                if (!screenActive) detailListState.stopScroll()
            }
            LaunchedEffect(scrollToTopRequests) {
                scrollToTopRequests.collect {
                    detailListState.animateScrollToItem(0)
                }
            }
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(tokens.colors.background)
                    .nuvioContentEntry(),
            ) {
                val ambientAlpha = if (tokens.colors.background == Color.Black) 0.03f else 0.055f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    tokens.colors.accent.copy(alpha = ambientAlpha),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                val baseHorizontalPadding = NuvioTokens.Space.s36
                val maxContentWidth = 1040.dp
                val contentHorizontalPadding = if (maxWidth > maxContentWidth + baseHorizontalPadding * 2) {
                    (maxWidth - maxContentWidth) / 2
                } else {
                    baseHorizontalPadding
                }
                LazyColumn(
                    state = detailListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .desktopSmoothWheelScroll(detailListState),
                    contentPadding = PaddingValues(
                        start = contentHorizontalPadding,
                        top = detailTopOffset,
                        end = contentHorizontalPadding,
                        bottom = NuvioTokens.Space.s40 + bottomOverlayPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s18),
                ) {
                    if (isSearchActive) {
                        item(key = "desktop-search-header") {
                            DesktopSettingsPageHeader(
                                title = stringResource(Res.string.settings_search_results_section),
                                subtitle = stringResource(
                                    Res.string.settings_search_results_for,
                                    settingsSearchQuery.trim(),
                                ),
                                icon = Icons.Rounded.Search,
                            )
                        }
                        settingsSearchRootContent(
                            query = settingsSearchQuery,
                            entries = {
                                settingsSearchEntries(
                                    isTablet = true,
                                    pluginsEnabled = AppFeaturePolicy.pluginsEnabled,
                                    notificationsEnabled = AppFeaturePolicy.notificationsEnabled,
                                    externalPlayerSupported = AppFeaturePolicy.externalPlayerSupported,
                                    supportersContributorsPageEnabled = AppFeaturePolicy.supportersContributorsPageEnabled,
                                    accountDeletionEnabled = AppFeaturePolicy.accountDeletionEnabled,
                                    personalMediaAddonCopyEnabled = AppFeaturePolicy.personalMediaAddonCopyEnabled,
                                    liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                                    switchProfileAvailable = onSwitchProfile != null,
                                    checkForUpdatesAvailable = onCheckForUpdatesClick != null,
                                )
                            },
                            isTablet = true,
                            showSearchField = false,
                            animateSearchField = false,
                            allowSearchField = false,
                            onQueryChange = { settingsSearchQuery = it },
                            onTargetClick = ::handleSearchTarget,
                        )
                    } else {
                        if (showInternalHeader) {
                            item(key = "desktop-page-header") {
                                val subDetail = route.subDetailPage
                                if (subDetail != null) {
                                    val onParentClick: (() -> Unit)? = parentPage?.let { targetParent ->
                                        { onPageChange(targetParent) }
                                    }
                                    DesktopSettingsPageHeader(
                                        title = stringResource(subDetail.titleRes),
                                        subtitle = desktopSettingsSubtitleRes(subDetail)?.let { stringResource(it) },
                                        icon = subDetail.icon,
                                        parentTitle = parentPage?.let {
                                            stringResource(resolveDesktopWorkspaceRoute(it).workspace.titleRes)
                                        },
                                        onParentClick = onParentClick,
                                    )
                                } else {
                                    DesktopSettingsPageHeader(
                                        title = stringResource(route.workspace.titleRes),
                                        subtitle = stringResource(route.workspace.subtitleRes),
                                        icon = route.workspace.icon,
                                    )
                                }
                            }
                        }
                        if (route.subDetailPage == null && workspaceTabs.size > 1) {
                            item(key = "desktop-workspace-tabs") {
                                val onTabSelected: (DesktopSettingsWorkspaceTab) -> Unit = { tab ->
                                    if (route.workspace == DesktopSettingsWorkspace.SystemAndPerformance &&
                                        tab.targetPage == SettingsPage.Advanced
                                    ) {
                                        systemTabOverride = tab
                                        if (page != SettingsPage.Advanced) {
                                            onPageChange(SettingsPage.Advanced)
                                        }
                                    } else {
                                        systemTabOverride = null
                                        onPageChange(tab.targetPage)
                                    }
                                }
                                DesktopSettingsTabBar(
                                    tabs = workspaceTabs,
                                    selectedTab = activeTab,
                                    onTabSelected = onTabSelected,
                                )
                            }
                        }
                        desktopSettingsWorkspaceContent(
                            route = route,
                            activeTab = activeTab,
                            onPageChange = onPageChange,
                            onNavigateBack = onNavigateBack,
                            rememberLastProfileEnabled = rememberLastProfileEnabled,
                            selectedTheme = selectedTheme,
                            onThemeSelected = onThemeSelected,
                            amoledEnabled = amoledEnabled,
                            onAmoledToggle = onAmoledToggle,
                            liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                            appIconState = appIconState,
                            onAppIconSelected = onAppIconSelected,
                            onAppIconFailureDismissed = onAppIconFailureDismissed,
                            selectedAppLanguage = selectedAppLanguage,
                            onAppLanguageSelected = onAppLanguageSelected,
                            episodeReleaseNotificationsUiState = episodeReleaseNotificationsUiState,
                            tmdbSettings = tmdbSettings,
                            mdbListSettings = mdbListSettings,
                            debridSettings = debridSettings,
                            traktAuthUiState = traktAuthUiState,
                            simklAuthUiState = simklAuthUiState,
                            traktCommentsEnabled = traktCommentsEnabled,
                            mdbListConnected = mdbListConnected,
                            trackingSettingsUiState = trackingSettingsUiState,
                            homescreenHeroEnabled = homescreenHeroEnabled,
                            homescreenShowCatalogType = homescreenShowCatalogType,
                            homescreenHideUnreleasedContent = homescreenHideUnreleasedContent,
                            homescreenItems = homescreenItems,
                            homescreenCatalogLoading = homescreenCatalogLoading,
                            homescreenCatalogErrorMessage = homescreenCatalogErrorMessage,
                            metaScreenSettingsUiState = metaScreenSettingsUiState,
                            continueWatchingPreferencesUiState = continueWatchingPreferencesUiState,
                            posterCardStyleUiState = posterCardStyleUiState,
                            onSwitchProfile = onSwitchProfile,
                            onWatchPartyClick = watchPartyRowClick,
                            onCheckForUpdatesClick = onCheckForUpdatesClick,
                            onTestUpdateBannerClick = onTestUpdateBannerClick,
                            onCollectionsClick = onCollectionsClick,
                        )
                    }
                }
                NuvioDesktopVerticalScrollbar(
                    state = detailListState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = detailTopOffset, bottom = 8.dp, end = 4.dp),
                )
            }
        }
    }
}

private fun LazyListScope.desktopSettingsWorkspaceContent(
    route: DesktopSettingsWorkspaceRoute,
    activeTab: DesktopSettingsWorkspaceTab?,
    onPageChange: (SettingsPage) -> Unit,
    onNavigateBack: () -> Unit,
    rememberLastProfileEnabled: Boolean,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    amoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    liquidGlassNativeTabBarSupported: Boolean,
    appIconState: AppIconSettingsState,
    onAppIconSelected: (AppIconOption) -> Unit,
    onAppIconFailureDismissed: () -> Unit,
    selectedAppLanguage: AppLanguage,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    episodeReleaseNotificationsUiState: EpisodeReleaseNotificationsUiState,
    tmdbSettings: TmdbSettings,
    mdbListSettings: MdbListSettings,
    debridSettings: DebridSettings,
    traktAuthUiState: TraktAuthUiState,
    simklAuthUiState: SimklAuthUiState,
    traktCommentsEnabled: Boolean,
    mdbListConnected: Boolean,
    trackingSettingsUiState: TrackingSettingsUiState,
    homescreenHeroEnabled: Boolean,
    homescreenShowCatalogType: Boolean,
    homescreenHideUnreleasedContent: Boolean,
    homescreenItems: List<HomeCatalogSettingsItem>,
    homescreenCatalogLoading: Boolean,
    homescreenCatalogErrorMessage: String?,
    metaScreenSettingsUiState: MetaScreenSettingsUiState,
    continueWatchingPreferencesUiState: ContinueWatchingPreferencesUiState,
    posterCardStyleUiState: PosterCardStyleUiState,
    onSwitchProfile: (() -> Unit)?,
    onWatchPartyClick: (() -> Unit)?,
    onCheckForUpdatesClick: (() -> Unit)?,
    onTestUpdateBannerClick: (() -> Unit)?,
    onCollectionsClick: () -> Unit,
) {
    when (route.workspace) {
        DesktopSettingsWorkspace.AppearanceAndNav -> {
            appearanceSettingsContent(
                isTablet = true,
                selectedTheme = selectedTheme,
                onThemeSelected = onThemeSelected,
                amoledEnabled = amoledEnabled,
                onAmoledToggle = onAmoledToggle,
                appIconState = appIconState,
                onAppIconSelected = onAppIconSelected,
                onAppIconFailureDismissed = onAppIconFailureDismissed,
                selectedAppLanguage = selectedAppLanguage,
                onAppLanguageSelected = onAppLanguageSelected,
            )
            item(key = "desktop-appearance-navigation") {
                LayoutNavigationBarSection(
                    isTablet = true,
                    liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
                )
            }
        }
        DesktopSettingsWorkspace.HomeAndCards -> when (activeTab) {
            DesktopSettingsWorkspaceTab.ContinueWatching -> continueWatchingSettingsContent(
                isTablet = true,
                isVisible = continueWatchingPreferencesUiState.isVisible,
                style = continueWatchingPreferencesUiState.style,
                upNextFromFurthestEpisode = continueWatchingPreferencesUiState.upNextFromFurthestEpisode,
                useEpisodeThumbnails = continueWatchingPreferencesUiState.useEpisodeThumbnails,
                showUnairedNextUp = continueWatchingPreferencesUiState.showUnairedNextUp,
                blurNextUp = continueWatchingPreferencesUiState.blurNextUp,
                showResumePromptOnLaunch = continueWatchingPreferencesUiState.showResumePromptOnLaunch,
                sortMode = continueWatchingPreferencesUiState.sortMode,
                clickAction = continueWatchingPreferencesUiState.clickAction,
            )
            DesktopSettingsWorkspaceTab.PostersAndPreview -> {
                posterCustomizationSettingsContent(
                    isTablet = true,
                    uiState = posterCardStyleUiState,
                )
                hoverPreviewSettingsContent(
                    isTablet = true,
                    uiState = posterCardStyleUiState,
                )
            }
            DesktopSettingsWorkspaceTab.DetailsPage -> metaScreenSettingsContent(
                isTablet = true,
                uiState = metaScreenSettingsUiState,
            )
            else -> homescreenSettingsContent(
                isTablet = true,
                heroEnabled = homescreenHeroEnabled,
                showCatalogType = homescreenShowCatalogType,
                hideUnreleasedContent = homescreenHideUnreleasedContent,
                items = homescreenItems,
                isCatalogLoading = homescreenCatalogLoading,
                catalogErrorMessage = homescreenCatalogErrorMessage,
            )
        }
        DesktopSettingsWorkspace.PlaybackAndStreams -> desktopPlaybackTabContent(
            tab = activeTab ?: DesktopSettingsWorkspaceTab.PlayerAndVideo,
            isTablet = true,
        )
        DesktopSettingsWorkspace.AddonsAndPlugins -> when (activeTab) {
            DesktopSettingsWorkspaceTab.Plugins -> if (AppFeaturePolicy.pluginsEnabled) {
                pluginsSettingsContent()
            } else {
                addonsSettingsContent()
            }
            else -> addonsSettingsContent()
        }
        DesktopSettingsWorkspace.ServersAndCollections -> when (route.subDetailPage) {
            SettingsPage.MediaServer -> mediaServerSettingsContent(
                isTablet = true,
                onBack = onNavigateBack,
            )
            else -> mediaServersSettingsContent(
                isTablet = true,
                onServerClick = { onPageChange(SettingsPage.MediaServer) },
                onCollectionsClick = onCollectionsClick,
            )
        }
        DesktopSettingsWorkspace.TrackingAndSync -> trackingSettingsContent(
            isTablet = true,
            traktUiState = traktAuthUiState,
            simklUiState = simklAuthUiState,
            settingsUiState = trackingSettingsUiState,
            commentsEnabled = traktCommentsEnabled,
            mdbListConnected = mdbListConnected,
            onCommentsEnabledChange = TraktCommentsSettings::setEnabled,
        )
        DesktopSettingsWorkspace.MetadataAndRatings -> when (activeTab) {
            DesktopSettingsWorkspaceTab.MdbList -> mdbListSettingsContent(
                isTablet = true,
                settings = mdbListSettings,
            )
            else -> tmdbSettingsContent(
                isTablet = true,
                settings = tmdbSettings,
            )
        }
        DesktopSettingsWorkspace.DebridServices -> debridSettingsContent(
            isTablet = true,
            settings = debridSettings,
        )
        DesktopSettingsWorkspace.AccountAndProfiles -> accountSettingsContent(
            isTablet = true,
            onSwitchProfile = onSwitchProfile,
            onWatchPartyClick = onWatchPartyClick,
        )
        DesktopSettingsWorkspace.SystemAndPerformance -> when (activeTab) {
            DesktopSettingsWorkspaceTab.Notifications -> notificationsSettingsContent(
                isTablet = true,
                uiState = episodeReleaseNotificationsUiState,
            )
            else -> advancedSettingsContent(
                isTablet = true,
                rememberLastProfileEnabled = rememberLastProfileEnabled,
                desktopTab = activeTab,
            )
        }
        DesktopSettingsWorkspace.AboutAndUpdates -> when (route.subDetailPage) {
            SettingsPage.SupportersContributors -> {
                if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                    supportersContributorsContent(isTablet = true)
                }
            }
            SettingsPage.LicensesAttributions -> licensesAttributionsContent(
                isTablet = true,
            )
            else -> aboutSettingsContent(
                isTablet = true,
                onSupportersContributorsClick = { onPageChange(SettingsPage.SupportersContributors) },
                onLicensesAttributionsClick = { onPageChange(SettingsPage.LicensesAttributions) },
                onCheckForUpdatesClick = onCheckForUpdatesClick,
                onTestUpdateBannerClick = onTestUpdateBannerClick,
                showSupportersContributorsPage = AppFeaturePolicy.supportersContributorsPageEnabled,
            )
        }
    }
}

private fun LazyListScope.settingsDetailPageContent(
    page: SettingsPage,
    onPageChange: (SettingsPage) -> Unit,
    onNavigateBack: () -> Unit,
    rememberLastProfileEnabled: Boolean,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    amoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    liquidGlassNativeTabBarSupported: Boolean,
    appIconState: AppIconSettingsState,
    onAppIconSelected: (AppIconOption) -> Unit,
    onAppIconFailureDismissed: () -> Unit,
    selectedAppLanguage: AppLanguage,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    episodeReleaseNotificationsUiState: EpisodeReleaseNotificationsUiState,
    tmdbSettings: TmdbSettings,
    mdbListSettings: MdbListSettings,
    debridSettings: DebridSettings,
    traktAuthUiState: TraktAuthUiState,
    simklAuthUiState: SimklAuthUiState,
    traktCommentsEnabled: Boolean,
    mdbListConnected: Boolean,
    trackingSettingsUiState: TrackingSettingsUiState,
    homescreenHeroEnabled: Boolean,
    homescreenShowCatalogType: Boolean,
    homescreenHideUnreleasedContent: Boolean,
    homescreenItems: List<HomeCatalogSettingsItem>,
    homescreenCatalogLoading: Boolean,
    homescreenCatalogErrorMessage: String?,
    metaScreenSettingsUiState: MetaScreenSettingsUiState,
    continueWatchingPreferencesUiState: ContinueWatchingPreferencesUiState,
    posterCardStyleUiState: PosterCardStyleUiState,
    onCheckForUpdatesClick: (() -> Unit)?,
    onTestUpdateBannerClick: (() -> Unit)?,
    onCollectionsClick: () -> Unit,
) {
    when (page) {
        SettingsPage.Root -> Unit
        SettingsPage.Account -> accountSettingsContent(
            isTablet = true,
        )
        SettingsPage.SupportersContributors -> {
            if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                supportersContributorsContent(isTablet = true)
            }
        }
        SettingsPage.LicensesAttributions -> licensesAttributionsContent(
            isTablet = true,
        )
        SettingsPage.Playback,
        SettingsPage.PlaybackPlayer,
        SettingsPage.PlaybackStreamSelection,
        SettingsPage.PlaybackUpNext,
        SettingsPage.PlaybackSkipSegments,
        SettingsPage.PlaybackInterface,
        SettingsPage.PlaybackAudio,
        SettingsPage.PlaybackSubtitles,
        SettingsPage.PlaybackVideo,
        SettingsPage.PlaybackBuffer,
        SettingsPage.PlaybackP2p -> playbackSettingsContent(
            isTablet = true,
            page = page,
            onPageChange = onPageChange,
        )
        SettingsPage.Streams -> streamsSettingsContent(
            isTablet = true,
        )
        SettingsPage.Appearance -> appearanceSettingsContent(
            isTablet = true,
            selectedTheme = selectedTheme,
            onThemeSelected = onThemeSelected,
            amoledEnabled = amoledEnabled,
            onAmoledToggle = onAmoledToggle,
            appIconState = appIconState,
            onAppIconSelected = onAppIconSelected,
            onAppIconFailureDismissed = onAppIconFailureDismissed,
            selectedAppLanguage = selectedAppLanguage,
            onAppLanguageSelected = onAppLanguageSelected,
        )
        SettingsPage.Layout -> layoutSettingsContent(
            isTablet = true,
            liquidGlassNativeTabBarSupported = liquidGlassNativeTabBarSupported,
            onHomescreenClick = { onPageChange(SettingsPage.Homescreen) },
            onMetaScreenClick = { onPageChange(SettingsPage.MetaScreen) },
            onStreamsClick = { onPageChange(SettingsPage.Streams) },
            onCollectionsClick = onCollectionsClick,
            onContinueWatchingClick = { onPageChange(SettingsPage.ContinueWatching) },
            onPosterCustomizationClick = { onPageChange(SettingsPage.PosterCustomization) },
            onHoverPreviewClick = { onPageChange(SettingsPage.HoverPreview) },
        )
        SettingsPage.HoverPreview -> hoverPreviewSettingsContent(
            isTablet = true,
            uiState = posterCardStyleUiState,
        )
        SettingsPage.About -> aboutSettingsContent(
            isTablet = true,
            onSupportersContributorsClick = { onPageChange(SettingsPage.SupportersContributors) },
            onLicensesAttributionsClick = { onPageChange(SettingsPage.LicensesAttributions) },
            onCheckForUpdatesClick = onCheckForUpdatesClick,
            onTestUpdateBannerClick = onTestUpdateBannerClick,
            showSupportersContributorsPage = AppFeaturePolicy.supportersContributorsPageEnabled,
        )
        SettingsPage.Advanced -> advancedSettingsContent(
            isTablet = true,
            rememberLastProfileEnabled = rememberLastProfileEnabled,
        )
        SettingsPage.Notifications -> notificationsSettingsContent(
            isTablet = true,
            uiState = episodeReleaseNotificationsUiState,
        )
        SettingsPage.ContinueWatching -> continueWatchingSettingsContent(
            isTablet = true,
            isVisible = continueWatchingPreferencesUiState.isVisible,
            style = continueWatchingPreferencesUiState.style,
            upNextFromFurthestEpisode = continueWatchingPreferencesUiState.upNextFromFurthestEpisode,
            useEpisodeThumbnails = continueWatchingPreferencesUiState.useEpisodeThumbnails,
            showUnairedNextUp = continueWatchingPreferencesUiState.showUnairedNextUp,
            blurNextUp = continueWatchingPreferencesUiState.blurNextUp,
            showResumePromptOnLaunch = continueWatchingPreferencesUiState.showResumePromptOnLaunch,
            sortMode = continueWatchingPreferencesUiState.sortMode,
            clickAction = continueWatchingPreferencesUiState.clickAction,
        )
        SettingsPage.PosterCustomization -> posterCustomizationSettingsContent(
            isTablet = true,
            uiState = posterCardStyleUiState,
        )
        SettingsPage.ContentDiscovery -> contentDiscoveryContent(
            isTablet = true,
            showPluginsEntry = AppFeaturePolicy.pluginsEnabled,
            onAddonsClick = { onPageChange(SettingsPage.Addons) },
            onPluginsClick = { onPageChange(SettingsPage.Plugins) },
            onMediaServersClick = { onPageChange(SettingsPage.MediaServers) },
        )
        SettingsPage.Addons -> addonsSettingsContent()
        SettingsPage.MediaServers -> mediaServersSettingsContent(
            isTablet = true,
            onServerClick = { onPageChange(SettingsPage.MediaServer) },
        )
        SettingsPage.MediaServer -> mediaServerSettingsContent(
            isTablet = true,
            onBack = onNavigateBack,
        )
        SettingsPage.Plugins -> if (AppFeaturePolicy.pluginsEnabled) pluginsSettingsContent() else addonsSettingsContent()
        SettingsPage.Homescreen -> homescreenSettingsContent(
            isTablet = true,
            heroEnabled = homescreenHeroEnabled,
            showCatalogType = homescreenShowCatalogType,
            hideUnreleasedContent = homescreenHideUnreleasedContent,
            items = homescreenItems,
            isCatalogLoading = homescreenCatalogLoading,
            catalogErrorMessage = homescreenCatalogErrorMessage,
        )
        SettingsPage.MetaScreen -> metaScreenSettingsContent(
            isTablet = true,
            uiState = metaScreenSettingsUiState,
        )
        SettingsPage.Integrations -> integrationsContent(
            isTablet = true,
            onTmdbClick = { onPageChange(SettingsPage.TmdbEnrichment) },
            onMdbListClick = { onPageChange(SettingsPage.MdbListRatings) },
            onDebridClick = { onPageChange(SettingsPage.Debrid) },
        )
        SettingsPage.TmdbEnrichment -> tmdbSettingsContent(
            isTablet = true,
            settings = tmdbSettings,
        )
        SettingsPage.MdbListRatings -> mdbListSettingsContent(
            isTablet = true,
            settings = mdbListSettings,
        )
        SettingsPage.Debrid -> debridSettingsContent(
            isTablet = true,
            settings = debridSettings,
        )
        SettingsPage.TraktAuthentication -> trackingSettingsContent(
            isTablet = true,
            traktUiState = traktAuthUiState,
            simklUiState = simklAuthUiState,
            settingsUiState = trackingSettingsUiState,
            commentsEnabled = traktCommentsEnabled,
            mdbListConnected = mdbListConnected,
            onCommentsEnabledChange = TraktCommentsSettings::setEnabled,
        )
    }
}
