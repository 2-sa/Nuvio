package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.firstEnabledManifestError
import com.nuvio.app.features.addons.hasPendingEnabledManifests
import com.nuvio.app.features.addons.isWaitingForFirstEnabledManifest
import com.nuvio.app.features.collection.CollectionRepository
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.buildAddonCatalogRefreshSignature
import com.nuvio.app.features.plugins.PluginRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_account
import nuvio.composeapp.generated.resources.compose_settings_page_addons
import nuvio.composeapp.generated.resources.compose_settings_page_continue_watching
import nuvio.composeapp.generated.resources.compose_settings_page_homescreen
import nuvio.composeapp.generated.resources.compose_settings_page_meta_screen
import nuvio.composeapp.generated.resources.compose_settings_page_plugins
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
private fun SettingsFullScreenScaffold(
    titleRes: StringResource,
    onBack: () -> Unit,
    content: LazyListScope.(isTablet: Boolean) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val title = stringResource(titleRes)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wideDesktop = isDesktop && maxWidth >= 960.dp
        val basePadding = if (isDesktop) NuvioTokens.Space.s24 else tokens.spacing.screenHorizontal
        val maxContentWidth = 820.dp
        val horizontalPadding = if (isDesktop && maxWidth > maxContentWidth + basePadding * 2) {
            (maxWidth - maxContentWidth) / 2
        } else {
            basePadding
        }
        NuvioScreen(
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = horizontalPadding,
        ) {
            stickyHeader {
                NuvioScreenHeader(
                    title = title,
                    onBack = onBack,
                )
            }
            content(wideDesktop)
        }
    }
}

@Composable
fun HomescreenSettingsScreen(
    onBack: () -> Unit,
) {
    val addonsUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
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

    LaunchedEffect(Unit) {
        AddonRepository.initialize()
        CollectionRepository.initialize()
    }

    LaunchedEffect(homescreenCatalogRefreshKey) {
        val enabledAddons = addonsUiState.addons.enabledAddons()
        if (!enabledAddons.isWaitingForFirstEnabledManifest()) {
            HomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
        }
    }

    LaunchedEffect(collections) {
        HomeCatalogSettingsRepository.syncCollections(collections)
    }

    SettingsFullScreenScaffold(
        titleRes = Res.string.compose_settings_page_homescreen,
        onBack = onBack,
    ) { isTablet ->
        homescreenSettingsContent(
            isTablet = isTablet,
            heroEnabled = homescreenSettingsUiState.heroEnabled,
            showCatalogType = homescreenSettingsUiState.showCatalogType,
            hideUnreleasedContent = homescreenSettingsUiState.hideUnreleasedContent,
            items = homescreenSettingsUiState.items,
            isCatalogLoading = addonManifestsLoading,
            catalogErrorMessage = addonManifestErrorMessage,
        )
    }
}

@Composable
fun MetaScreenSettingsScreen(
    onBack: () -> Unit,
) {
    val metaScreenSettingsUiState by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()

    SettingsFullScreenScaffold(
        titleRes = Res.string.compose_settings_page_meta_screen,
        onBack = onBack,
    ) { isTablet ->
        metaScreenSettingsContent(
            isTablet = isTablet,
            uiState = metaScreenSettingsUiState,
        )
    }
}

@Composable
fun ContinueWatchingSettingsScreen(
    onBack: () -> Unit,
) {
    val continueWatchingPreferencesUiState by remember {
        ContinueWatchingPreferencesRepository.ensureLoaded()
        ContinueWatchingPreferencesRepository.uiState
    }.collectAsStateWithLifecycle()

    SettingsFullScreenScaffold(
        titleRes = Res.string.compose_settings_page_continue_watching,
        onBack = onBack,
    ) { isTablet ->
        continueWatchingSettingsContent(
            isTablet = isTablet,
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
    }
}

@Composable
fun AddonsSettingsScreen(
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        AddonRepository.initialize()
    }

    SettingsFullScreenScaffold(
        titleRes = Res.string.compose_settings_page_addons,
        onBack = onBack,
    ) {
        addonsSettingsContent()
    }
}

@Composable
fun PluginsSettingsScreen(
    onBack: () -> Unit,
) {
    if (!AppFeaturePolicy.pluginsEnabled) {
        AddonsSettingsScreen(onBack = onBack)
        return
    }

    LaunchedEffect(Unit) {
        PluginRepository.initialize()
    }

    SettingsFullScreenScaffold(
        titleRes = Res.string.compose_settings_page_plugins,
        onBack = onBack,
    ) {
        pluginsSettingsContent()
    }
}

@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
) {
    SettingsFullScreenScaffold(
        titleRes = Res.string.compose_settings_page_account,
        onBack = onBack,
    ) { isTablet ->
        accountSettingsContent(
            isTablet = isTablet,
        )
    }
}
