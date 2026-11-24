package com.nuvio.app.features.plugins

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioIconActionButton
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.addons.isPrimarilyLatinText
import com.nuvio.app.features.plugins.runtime.PluginRuntime
import com.nuvio.app.isDesktop
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.addons_action_paste
import nuvio.composeapp.generated.resources.addons_stats_active
import nuvio.composeapp.generated.resources.addons_type_anime
import nuvio.composeapp.generated.resources.addons_type_channel
import nuvio.composeapp.generated.resources.addons_type_movie
import nuvio.composeapp.generated.resources.addons_type_other
import nuvio.composeapp.generated.resources.addons_type_series
import nuvio.composeapp.generated.resources.plugins_badge_disabled
import nuvio.composeapp.generated.resources.plugins_badge_enabled
import nuvio.composeapp.generated.resources.plugins_badge_providers
import nuvio.composeapp.generated.resources.plugins_badge_refreshing
import nuvio.composeapp.generated.resources.plugins_badge_repos
import nuvio.composeapp.generated.resources.plugins_button_install_repo
import nuvio.composeapp.generated.resources.plugins_button_installing
import nuvio.composeapp.generated.resources.plugins_button_test_provider
import nuvio.composeapp.generated.resources.plugins_button_testing
import nuvio.composeapp.generated.resources.plugins_cd_delete_repo
import nuvio.composeapp.generated.resources.plugins_cd_refresh_repo
import nuvio.composeapp.generated.resources.plugins_empty_providers
import nuvio.composeapp.generated.resources.plugins_empty_repos_subtitle
import nuvio.composeapp.generated.resources.plugins_empty_repos_title
import nuvio.composeapp.generated.resources.plugins_enable_globally_desc
import nuvio.composeapp.generated.resources.plugins_enable_globally_title
import nuvio.composeapp.generated.resources.plugins_error_enter_repo_url
import nuvio.composeapp.generated.resources.plugins_group_by_repo_desc
import nuvio.composeapp.generated.resources.plugins_group_by_repo_title
import nuvio.composeapp.generated.resources.plugins_input_manifest_placeholder
import nuvio.composeapp.generated.resources.plugins_message_installed
import nuvio.composeapp.generated.resources.plugins_provider_disabled_by_repo
import nuvio.composeapp.generated.resources.plugins_provider_no_description
import nuvio.composeapp.generated.resources.plugins_provider_version
import nuvio.composeapp.generated.resources.plugins_repo_fallback_label
import nuvio.composeapp.generated.resources.plugins_repo_version
import nuvio.composeapp.generated.resources.plugins_section_add_repo
import nuvio.composeapp.generated.resources.plugins_section_installed_repos
import nuvio.composeapp.generated.resources.plugins_section_overview
import nuvio.composeapp.generated.resources.plugins_section_providers
import nuvio.composeapp.generated.resources.plugins_test_error_title
import nuvio.composeapp.generated.resources.plugins_test_failed
import nuvio.composeapp.generated.resources.plugins_test_results_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PluginsSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        PluginRepository.initialize()
    }

    val uiState by PluginRepository.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var repositoryUrl by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    var testingScraperId by remember { mutableStateOf<String?>(null) }
    val testResults = remember { mutableStateMapOf<String, List<PluginRuntimeResult>>() }

    var configuringScraper by remember { mutableStateOf<PluginScraper?>(null) }
    var configuringLayout by remember { mutableStateOf<String?>(null) }

    val sortedRepos = remember(uiState.repositories) {
        uiState.repositories.sortedBy { it.name.lowercase() }
    }
    val repositoryNameByUrl = remember(sortedRepos) {
        sortedRepos.associate { it.manifestUrl to it.name }
    }
    val sortedScrapers = remember(uiState.scrapers, repositoryNameByUrl) {
        uiState.scrapers.sortedWith(
            compareBy<PluginScraper>(
                { repositoryNameByUrl[it.repositoryUrl]?.lowercase() ?: it.repositoryUrl.lowercase() },
                { it.name.lowercase() },
            ),
        )
    }

    val repoFallbackLabel = stringResource(Res.string.plugins_repo_fallback_label)
    val testFailedDefault = stringResource(Res.string.plugins_test_failed)
    val testErrorTitle = stringResource(Res.string.plugins_test_error_title)
    val installedTemplate = stringResource(Res.string.plugins_message_installed)
    val enterRepoUrlError = stringResource(Res.string.plugins_error_enter_repo_url)

    val onInstallRepository: () -> Unit = {
        val requested = repositoryUrl.trim()
        if (requested.isBlank()) {
            message = enterRepoUrlError
        } else if (!isAdding) {
            isAdding = true
            message = null
            coroutineScope.launch {
                when (val result = PluginRepository.addRepository(requested)) {
                    is AddPluginRepositoryResult.Success -> {
                        repositoryUrl = ""
                        message = installedTemplate.replace("%1\$s", result.repository.name)
                    }
                    is AddPluginRepositoryResult.Error -> {
                        message = result.message
                    }
                }
                isAdding = false
            }
        }
    }

    val onConfigureScraper: (PluginScraper) -> Unit = { scraper ->
        coroutineScope.launch {
            val layout = PluginRuntime.getPluginSettingsLayout(scraper.code, scraper.id)
            if (layout != null) {
                configuringScraper = scraper
                configuringLayout = layout
            }
        }
    }

    val onTestScraper: (PluginScraper) -> Unit = { scraper ->
        testingScraperId = scraper.id
        coroutineScope.launch {
            PluginRepository.testScraper(scraper.id)
                .onSuccess { results ->
                    testResults[scraper.id] = results
                }
                .onFailure { error ->
                    testResults[scraper.id] = listOf(
                        PluginRuntimeResult(
                            title = testErrorTitle,
                            name = error.message ?: testFailedDefault,
                            url = "about:error",
                        ),
                    )
                }
            testingScraperId = null
        }
    }

    if (isDesktop) {
        val activeProvidersCount = remember(sortedScrapers, uiState.pluginsEnabled) {
            if (uiState.pluginsEnabled) {
                sortedScrapers.count { it.enabled && it.manifestEnabled }
            } else {
                0
            }
        }

        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val isTwoColumn = maxWidth >= 680.dp

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Inline Stat Pills
                DesktopPluginsStatsStrip(
                    repoCount = sortedRepos.size,
                    activeProviderCount = activeProvidersCount,
                    totalProviderCount = sortedScrapers.size,
                    pluginsEnabled = uiState.pluginsEnabled,
                )

                // Global Plugin Switches in a clean 2-column desktop layout
                DesktopPluginGlobalSwitches(
                    pluginsEnabled = uiState.pluginsEnabled,
                    groupStreamsByRepository = uiState.groupStreamsByRepository,
                    isTwoColumn = isTwoColumn,
                    onPluginsEnabledChange = { PluginRepository.setPluginsEnabled(it) },
                    onGroupStreamsChange = { PluginRepository.setGroupStreamsByRepository(it) },
                )

                // Single-Line Repository Install Command Bar
                DesktopPluginInstallCommandBar(
                    repositoryUrl = repositoryUrl,
                    isAdding = isAdding,
                    message = message,
                    onRepositoryUrlChange = {
                        repositoryUrl = it
                        message = null
                    },
                    onInstallClick = onInstallRepository,
                )

                // Installed Repositories
                NuvioSectionLabel(stringResource(Res.string.plugins_section_installed_repos))
                if (sortedRepos.isEmpty()) {
                    DesktopPluginEmptyCard(
                        title = stringResource(Res.string.plugins_empty_repos_title),
                        subtitle = stringResource(Res.string.plugins_empty_repos_subtitle),
                    )
                } else if (isTwoColumn) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        sortedRepos.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                pair.forEach { repo ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        DesktopPluginRepositoryCard(
                                            repo = repo,
                                            onRefresh = {
                                                PluginRepository.refreshRepository(
                                                    repo.manifestUrl,
                                                    pushAfterRefresh = true,
                                                )
                                            },
                                            onDelete = {
                                                PluginRepository.removeRepository(repo.manifestUrl)
                                            },
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        sortedRepos.forEach { repo ->
                            DesktopPluginRepositoryCard(
                                repo = repo,
                                onRefresh = {
                                    PluginRepository.refreshRepository(
                                        repo.manifestUrl,
                                        pushAfterRefresh = true,
                                    )
                                },
                                onDelete = {
                                    PluginRepository.removeRepository(repo.manifestUrl)
                                },
                            )
                        }
                    }
                }

                // Providers
                NuvioSectionLabel(stringResource(Res.string.plugins_section_providers))
                if (sortedScrapers.isEmpty()) {
                    DesktopPluginEmptyCard(
                        title = null,
                        subtitle = stringResource(Res.string.plugins_empty_providers),
                    )
                } else if (isTwoColumn) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        sortedScrapers.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                pair.forEach { scraper ->
                                    val repositoryName = repositoryNameByUrl[scraper.repositoryUrl]
                                        ?: scraper.repositoryUrl.fallbackRepositoryLabel(repoFallbackLabel)
                                    Box(modifier = Modifier.weight(1f)) {
                                        DesktopPluginProviderCard(
                                            scraper = scraper,
                                            repositoryName = repositoryName,
                                            isTesting = testingScraperId == scraper.id,
                                            testResults = testResults[scraper.id],
                                            onToggle = { PluginRepository.toggleScraper(scraper.id, it) },
                                            onConfigure = { onConfigureScraper(scraper) },
                                            onTest = { onTestScraper(scraper) },
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        sortedScrapers.forEach { scraper ->
                            val repositoryName = repositoryNameByUrl[scraper.repositoryUrl]
                                ?: scraper.repositoryUrl.fallbackRepositoryLabel(repoFallbackLabel)
                            DesktopPluginProviderCard(
                                scraper = scraper,
                                repositoryName = repositoryName,
                                isTesting = testingScraperId == scraper.id,
                                testResults = testResults[scraper.id],
                                onToggle = { PluginRepository.toggleScraper(scraper.id, it) },
                                onConfigure = { onConfigureScraper(scraper) },
                                onTest = { onTestScraper(scraper) },
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NuvioSectionLabel(stringResource(Res.string.plugins_section_overview))
            NuvioSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_repos, sortedRepos.size))
                    NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_providers, sortedScrapers.size))
                    NuvioInfoBadge(
                        text = if (uiState.pluginsEnabled) {
                            stringResource(Res.string.plugins_badge_enabled)
                        } else {
                            stringResource(Res.string.plugins_badge_disabled)
                        },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.plugins_enable_globally_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.plugins_enable_globally_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = uiState.pluginsEnabled,
                        onCheckedChange = { PluginRepository.setPluginsEnabled(it) },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.plugins_group_by_repo_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.plugins_group_by_repo_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = uiState.groupStreamsByRepository,
                        onCheckedChange = { PluginRepository.setGroupStreamsByRepository(it) },
                    )
                }
            }

            NuvioSectionLabel(stringResource(Res.string.plugins_section_add_repo))
            NuvioSurfaceCard {
                NuvioInputField(
                    value = repositoryUrl,
                    onValueChange = {
                        repositoryUrl = it
                        message = null
                    },
                    placeholder = stringResource(Res.string.plugins_input_manifest_placeholder),
                )
                Spacer(modifier = Modifier.height(16.dp))
                NuvioPrimaryButton(
                    text = if (isAdding) {
                        stringResource(Res.string.plugins_button_installing)
                    } else {
                        stringResource(Res.string.plugins_button_install_repo)
                    },
                    enabled = repositoryUrl.isNotBlank() && !isAdding,
                    onClick = onInstallRepository,
                )
                message?.let { text ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            NuvioSectionLabel(stringResource(Res.string.plugins_section_installed_repos))
            if (sortedRepos.isEmpty()) {
                NuvioSurfaceCard {
                    Text(
                        text = stringResource(Res.string.plugins_empty_repos_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.plugins_empty_repos_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                sortedRepos.forEach { repo ->
                    NuvioSurfaceCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = repo.name,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                repo.version?.let { version ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(Res.string.plugins_repo_version, version),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = repo.manifestUrl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NuvioIconActionButton(
                                    icon = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(Res.string.plugins_cd_refresh_repo),
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = { PluginRepository.refreshRepository(repo.manifestUrl, pushAfterRefresh = true) },
                                )
                                NuvioIconActionButton(
                                    icon = Icons.Rounded.Delete,
                                    contentDescription = stringResource(Res.string.plugins_cd_delete_repo),
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = { PluginRepository.removeRepository(repo.manifestUrl) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_providers, repo.scraperCount))
                            if (repo.isRefreshing) {
                                NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_refreshing))
                            }
                        }
                        repo.errorMessage?.let { errorMessage ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            NuvioSectionLabel(stringResource(Res.string.plugins_section_providers))
            if (sortedScrapers.isEmpty()) {
                NuvioSurfaceCard {
                    Text(
                        text = stringResource(Res.string.plugins_empty_providers),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                sortedScrapers.forEach { scraper ->
                    val scraperResults = testResults[scraper.id]
                    val isTestingThisScraper = testingScraperId == scraper.id
                    val repositoryName = repositoryNameByUrl[scraper.repositoryUrl]
                        ?: scraper.repositoryUrl.fallbackRepositoryLabel(repoFallbackLabel)

                    NuvioSurfaceCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Extension,
                                    contentDescription = null,
                                    tint = if (scraper.enabled) Color(0xFF68B76A) else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = repositoryName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = scraper.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = scraper.description.ifBlank {
                                            stringResource(Res.string.plugins_provider_no_description)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (scraper.hasSettings) {
                                    IconButton(onClick = { onConfigureScraper(scraper) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = "Provider settings",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                Switch(
                                    checked = scraper.enabled,
                                    onCheckedChange = { PluginRepository.toggleScraper(scraper.id, it) },
                                    enabled = scraper.manifestEnabled,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            NuvioInfoBadge(text = scraper.supportedTypes.joinToString(" | "))
                            NuvioInfoBadge(text = stringResource(Res.string.plugins_provider_version, scraper.version))
                            if (!scraper.manifestEnabled) {
                                NuvioInfoBadge(text = stringResource(Res.string.plugins_provider_disabled_by_repo))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        NuvioPrimaryButton(
                            text = if (isTestingThisScraper) {
                                stringResource(Res.string.plugins_button_testing)
                            } else {
                                stringResource(Res.string.plugins_button_test_provider)
                            },
                            enabled = !isTestingThisScraper,
                            onClick = { onTestScraper(scraper) },
                        )

                        if (!scraperResults.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(Res.string.plugins_test_results_count, scraperResults.size),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            scraperResults.take(8).forEach { result ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = result.url,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (configuringScraper != null && configuringLayout != null) {
        PluginSettingsDialog(
            scraperId = configuringScraper!!.id,
            scraperName = configuringScraper!!.name,
            layoutJson = configuringLayout!!,
            onDismiss = {
                configuringScraper = null
                configuringLayout = null
            },
        )
    }
}

@Composable
private fun DesktopPluginsStatsStrip(
    repoCount: Int,
    activeProviderCount: Int,
    totalProviderCount: Int,
    pluginsEnabled: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DesktopPluginStatPill(
            text = stringResource(Res.string.plugins_badge_repos, repoCount),
        )
        DesktopPluginStatPill(
            text = pluralStringResource(Res.plurals.addons_stats_active, activeProviderCount, activeProviderCount),
            dotColor = if (pluginsEnabled && activeProviderCount > 0) Color(0xFF34D399) else tokens.colors.textMuted,
            accentHighlight = pluginsEnabled && activeProviderCount > 0,
        )
        DesktopPluginStatPill(
            text = stringResource(Res.string.plugins_badge_providers, totalProviderCount),
        )
        if (!pluginsEnabled) {
            DesktopPluginStatPill(
                text = stringResource(Res.string.plugins_badge_disabled),
                dotColor = tokens.colors.textMuted,
            )
        }
    }
}

@Composable
private fun DesktopPluginStatPill(
    text: String,
    dotColor: Color? = null,
    accentHighlight: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(999.dp)
    val bgColor = if (accentHighlight && dotColor != null) {
        dotColor.copy(alpha = 0.10f)
    } else {
        Color.White.copy(alpha = 0.045f)
    }
    val borderColor = if (accentHighlight && dotColor != null) {
        dotColor.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.09f)
    }

    Row(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .border(NuvioTokens.Border.thin, borderColor, shape)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (accentHighlight && dotColor != null) dotColor else tokens.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DesktopPluginGlobalSwitches(
    pluginsEnabled: Boolean,
    groupStreamsByRepository: Boolean,
    isTwoColumn: Boolean,
    onPluginsEnabledChange: (Boolean) -> Unit,
    onGroupStreamsChange: (Boolean) -> Unit,
) {
    if (isTwoColumn) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            DesktopPluginToggleCard(
                title = stringResource(Res.string.plugins_enable_globally_title),
                description = stringResource(Res.string.plugins_enable_globally_desc),
                checked = pluginsEnabled,
                onCheckedChange = onPluginsEnabledChange,
                modifier = Modifier.weight(1f),
            )
            DesktopPluginToggleCard(
                title = stringResource(Res.string.plugins_group_by_repo_title),
                description = stringResource(Res.string.plugins_group_by_repo_desc),
                checked = groupStreamsByRepository,
                onCheckedChange = onGroupStreamsChange,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DesktopPluginToggleCard(
                title = stringResource(Res.string.plugins_enable_globally_title),
                description = stringResource(Res.string.plugins_enable_globally_desc),
                checked = pluginsEnabled,
                onCheckedChange = onPluginsEnabledChange,
            )
            DesktopPluginToggleCard(
                title = stringResource(Res.string.plugins_group_by_repo_title),
                description = stringResource(Res.string.plugins_group_by_repo_desc),
                checked = groupStreamsByRepository,
                onCheckedChange = onGroupStreamsChange,
            )
        }
    }
}

@Composable
private fun DesktopPluginToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = tokens.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .scale(0.84f)
                .pointerHoverIcon(PointerIcon.Hand),
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.colors.onAccent,
                checkedTrackColor = tokens.colors.accent,
                uncheckedThumbColor = Color(0xFFE2E8F0),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.18f),
            ),
        )
    }
}

@Suppress("DEPRECATION")
@Composable
private fun DesktopPluginInstallCommandBar(
    repositoryUrl: String,
    isAdding: Boolean,
    message: String?,
    onRepositoryUrlChange: (String) -> Unit,
    onInstallClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val clipboardManager = LocalClipboardManager.current
    val barShape = RoundedCornerShape(14.dp)
    val pasteShape = RoundedCornerShape(8.dp)
    val ctaShape = RoundedCornerShape(10.dp)

    val barInteraction = remember { MutableInteractionSource() }
    val isBarHovered by barInteraction.collectIsHoveredAsState()
    var isInputFocused by remember { mutableStateOf(false) }

    val pasteInteraction = remember { MutableInteractionSource() }
    val isPasteHovered by pasteInteraction.collectIsHoveredAsState()

    val ctaInteraction = remember { MutableInteractionSource() }
    val isCtaHovered by ctaInteraction.collectIsHoveredAsState()
    val ctaEnabled = repositoryUrl.isNotBlank() && !isAdding

    val barBorderColor by animateColorAsState(
        targetValue = when {
            isInputFocused -> tokens.colors.accent.copy(alpha = 0.72f)
            isBarHovered -> Color.White.copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.09f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "pluginInstallBarBorder",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(barShape)
                .background(Color.White.copy(alpha = 0.035f))
                .border(NuvioTokens.Border.thin, barBorderColor, barShape)
                .hoverable(barInteraction)
                .padding(start = 13.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = if (isInputFocused) tokens.colors.accent else tokens.colors.textMuted,
                modifier = Modifier.size(18.dp),
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (repositoryUrl.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.plugins_input_manifest_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = repositoryUrl,
                    onValueChange = onRepositoryUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isInputFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (
                                event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                if (!isAdding && repositoryUrl.isNotBlank()) {
                                    onInstallClick()
                                }
                                true
                            } else {
                                false
                            }
                        },
                    singleLine = true,
                    enabled = !isAdding,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        color = tokens.colors.textPrimary,
                        textDirection = TextDirection.Ltr,
                    ),
                    cursorBrush = SolidColor(tokens.colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isAdding && repositoryUrl.isNotBlank()) {
                                onInstallClick()
                            }
                        },
                    ),
                )
            }

            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(pasteShape)
                    .background(
                        if (isPasteHovered) {
                            Color.White.copy(alpha = 0.10f)
                        } else {
                            Color.White.copy(alpha = 0.055f)
                        },
                    )
                    .border(NuvioTokens.Border.thin, Color.White.copy(alpha = 0.09f), pasteShape)
                    .nuvioFocusBorder(pasteShape)
                    .hoverable(pasteInteraction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        enabled = !isAdding,
                        interactionSource = pasteInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                            if (pasted.isNotEmpty()) {
                                onRepositoryUrlChange(pasted)
                            }
                        },
                    )
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentPaste,
                    contentDescription = null,
                    tint = tokens.colors.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(Res.string.addons_action_paste),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = tokens.colors.textSecondary,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier
                    .height(36.dp)
                    .alpha(if (ctaEnabled || isAdding) 1f else tokens.opacity.disabled)
                    .clip(ctaShape)
                    .background(palette.accentBrush())
                    .then(
                        if (ctaEnabled && isCtaHovered) {
                            Modifier.background(Color.White.copy(alpha = NuvioTokens.Opacity.hover))
                        } else {
                            Modifier
                        },
                    )
                    .nuvioFocusBorder(ctaShape)
                    .then(
                        if (ctaEnabled) {
                            Modifier
                                .hoverable(ctaInteraction)
                                .pointerHoverIcon(PointerIcon.Hand)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        enabled = ctaEnabled,
                        interactionSource = ctaInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onInstallClick,
                    )
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isAdding) {
                    NuvioLoadingIndicator(
                        color = tokens.colors.onAccent,
                        size = 15.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = tokens.colors.onAccent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = if (isAdding) {
                        stringResource(Res.string.plugins_button_installing)
                    } else {
                        stringResource(Res.string.plugins_button_install_repo)
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = tokens.colors.onAccent,
                    maxLines = 1,
                )
            }
        }

        message?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = tokens.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun DesktopPluginEmptyCard(
    title: String?,
    subtitle: String,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, Color.White.copy(alpha = 0.08f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = tokens.colors.textPrimary,
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = tokens.colors.textSecondary,
        )
    }
}

@Composable
private fun DesktopPluginRepositoryCard(
    repo: PluginRepositoryItem,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isHovered) {
            tokens.colors.accent.copy(alpha = 0.40f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktopPluginRepoBorder",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, borderColor, cardShape)
            .hoverable(interactionSource)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = repo.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textDirection = if (isPrimarilyLatinText(repo.name)) {
                                TextDirection.Ltr
                            } else {
                                TextDirection.Content
                            },
                        ),
                        color = tokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    repo.version?.takeIf { it.isNotBlank() }?.let { version ->
                        Text(
                            text = "v$version",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                textDirection = TextDirection.Ltr,
                            ),
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = repo.manifestUrl,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        textDirection = TextDirection.Ltr,
                    ),
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopPluginCompactIconButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = stringResource(Res.string.plugins_cd_refresh_repo),
                    tint = tokens.colors.textSecondary,
                    hoverTint = tokens.colors.accent,
                    onClick = onRefresh,
                )
                DesktopPluginCompactIconButton(
                    icon = Icons.Rounded.Delete,
                    contentDescription = stringResource(Res.string.plugins_cd_delete_repo),
                    tint = Color(0xFFFB7185).copy(alpha = 0.85f),
                    hoverTint = Color(0xFFFB7185),
                    destructive = true,
                    onClick = onDelete,
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DesktopPluginMetaPill(
                text = stringResource(Res.string.plugins_badge_providers, repo.scraperCount),
            )
            if (repo.isRefreshing) {
                DesktopPluginMetaPill(
                    text = stringResource(Res.string.plugins_badge_refreshing),
                    accent = true,
                )
            }
        }

        repo.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = tokens.colors.danger,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DesktopPluginProviderCard(
    scraper: PluginScraper,
    repositoryName: String,
    isTesting: Boolean,
    testResults: List<PluginRuntimeResult>?,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    onTest: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(14.dp)
    val iconShape = RoundedCornerShape(9.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isHovered) {
            tokens.colors.accent.copy(alpha = 0.40f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktopPluginProviderBorder",
    )

    val localizedTypes = scraper.supportedTypes.mapNotNull { rawType ->
        when (rawType.trim().lowercase()) {
            "" -> null
            "movie", "movies" -> stringResource(Res.string.addons_type_movie)
            "tv", "series", "show", "shows" -> stringResource(Res.string.addons_type_series)
            "anime" -> stringResource(Res.string.addons_type_anime)
            "channel", "channels" -> stringResource(Res.string.addons_type_channel)
            "other" -> stringResource(Res.string.addons_type_other)
            else -> rawType.trim().replaceFirstChar(Char::uppercase)
        }
    }.distinct()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, borderColor, cardShape)
            .hoverable(interactionSource)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(iconShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(NuvioTokens.Border.thin, Color.White.copy(alpha = 0.09f), iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Extension,
                    contentDescription = null,
                    tint = if (scraper.enabled && scraper.manifestEnabled) {
                        Color(0xFF34D399)
                    } else {
                        tokens.colors.textMuted
                    },
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = scraper.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textDirection = if (isPrimarilyLatinText(scraper.name)) {
                            TextDirection.Ltr
                        } else {
                            TextDirection.Content
                        },
                    ),
                    color = tokens.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = repositoryName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            textDirection = if (isPrimarilyLatinText(repositoryName)) {
                                TextDirection.Ltr
                            } else {
                                TextDirection.Content
                            },
                        ),
                        color = tokens.colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        color = tokens.colors.textMuted,
                    )
                    Text(
                        text = stringResource(Res.string.plugins_provider_version, scraper.version),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            textDirection = TextDirection.Ltr,
                        ),
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (scraper.hasSettings) {
                    DesktopPluginCompactIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Provider settings",
                        tint = tokens.colors.textSecondary,
                        hoverTint = tokens.colors.accent,
                        onClick = onConfigure,
                    )
                }
                Switch(
                    checked = scraper.enabled,
                    onCheckedChange = onToggle,
                    enabled = scraper.manifestEnabled,
                    modifier = Modifier
                        .scale(0.84f)
                        .then(if (scraper.manifestEnabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = tokens.colors.onAccent,
                        checkedTrackColor = tokens.colors.accent,
                        uncheckedThumbColor = Color(0xFFE2E8F0),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.18f),
                        disabledCheckedThumbColor = tokens.colors.onAccent.copy(alpha = tokens.opacity.disabled),
                        disabledCheckedTrackColor = tokens.colors.accent.copy(alpha = tokens.opacity.disabled),
                        disabledUncheckedThumbColor = tokens.colors.textDisabled,
                        disabledUncheckedTrackColor = Color.White.copy(alpha = 0.05f),
                        disabledUncheckedBorderColor = Color.White.copy(alpha = 0.08f),
                    ),
                )
            }
        }

        // Description
        val descriptionText = scraper.description.ifBlank {
            stringResource(Res.string.plugins_provider_no_description)
        }
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                textDirection = if (isPrimarilyLatinText(descriptionText)) {
                    TextDirection.Ltr
                } else {
                    TextDirection.Content
                },
            ),
            color = tokens.colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        HorizontalDivider(
            thickness = NuvioTokens.Border.hairline,
            color = Color.White.copy(alpha = 0.06f),
        )

        // Footer Row: badges + compact capsule Test button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                localizedTypes.forEach { label ->
                    DesktopPluginMetaPill(text = label)
                }
                if (!scraper.manifestEnabled) {
                    DesktopPluginMetaPill(
                        text = stringResource(Res.string.plugins_provider_disabled_by_repo),
                        danger = true,
                    )
                }
            }

            DesktopPluginTestCapsuleButton(
                isTesting = isTesting,
                onClick = onTest,
            )
        }

        // Test results
        if (!testResults.isNullOrEmpty()) {
            HorizontalDivider(
                thickness = NuvioTokens.Border.hairline,
                color = Color.White.copy(alpha = 0.06f),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(Res.string.plugins_test_results_count, testResults.size),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = tokens.colors.textPrimary,
                )
                testResults.take(8).forEach { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = tokens.colors.accent,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(14.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                text = result.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDirection = if (isPrimarilyLatinText(result.title)) {
                                        TextDirection.Ltr
                                    } else {
                                        TextDirection.Content
                                    },
                                ),
                                color = tokens.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = result.url,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    textDirection = TextDirection.Ltr,
                                ),
                                color = tokens.colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopPluginMetaPill(
    text: String,
    accent: Boolean = false,
    danger: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(6.dp)
    val bgColor = when {
        danger -> Color(0xFFFB7185).copy(alpha = 0.12f)
        accent -> tokens.colors.accent.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.05f)
    }
    val borderColor = when {
        danger -> Color(0xFFFB7185).copy(alpha = 0.30f)
        accent -> tokens.colors.accent.copy(alpha = 0.28f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    val textColor = when {
        danger -> Color(0xFFFB7185)
        accent -> tokens.colors.accent
        else -> tokens.colors.textSecondary
    }

    Box(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .border(NuvioTokens.Border.thin, borderColor, shape)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DesktopPluginCompactIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    hoverTint: Color = MaterialTheme.nuvio.colors.textPrimary,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(7.dp)
    val bgColor by animateColorAsState(
        targetValue = when {
            isHovered && destructive -> Color(0xFFFB7185).copy(alpha = 0.16f)
            isHovered -> Color.White.copy(alpha = 0.11f)
            else -> Color.White.copy(alpha = 0.045f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktopPluginIconBtnBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isHovered && destructive -> Color(0xFFFB7185).copy(alpha = 0.45f)
            isHovered -> Color.White.copy(alpha = 0.18f)
            else -> Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktopPluginIconBtnBorder",
    )

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(shape)
            .background(bgColor)
            .border(NuvioTokens.Border.thin, borderColor, shape)
            .nuvioFocusBorder(shape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isHovered) hoverTint else tint,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun DesktopPluginTestCapsuleButton(
    isTesting: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(999.dp)
    val bgColor by animateColorAsState(
        targetValue = when {
            isTesting -> tokens.colors.accent.copy(alpha = 0.14f)
            isHovered -> tokens.colors.accent.copy(alpha = 0.22f)
            else -> tokens.colors.accent.copy(alpha = 0.12f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktopPluginTestBtnBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHovered && !isTesting) {
            tokens.colors.accent.copy(alpha = 0.55f)
        } else {
            tokens.colors.accent.copy(alpha = 0.30f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktopPluginTestBtnBorder",
    )

    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(shape)
            .background(bgColor)
            .border(NuvioTokens.Border.thin, borderColor, shape)
            .nuvioFocusBorder(shape)
            .then(
                if (!isTesting) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = !isTesting,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isTesting) {
            NuvioLoadingIndicator(
                color = tokens.colors.accent,
                size = 13.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = tokens.colors.accent,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = if (isTesting) {
                stringResource(Res.string.plugins_button_testing)
            } else {
                stringResource(Res.string.plugins_button_test_provider)
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = tokens.colors.accent,
            maxLines = 1,
        )
    }
}

private fun String.fallbackRepositoryLabel(fallback: String): String {
    val withoutQuery = substringBefore("?")
    val withoutManifest = withoutQuery.removeSuffix("/manifest.json")
    val host = withoutManifest.substringAfter("://", withoutManifest).substringBefore('/')
    return host.ifBlank {
        withoutManifest.substringAfterLast('/').ifBlank { fallback }
    }
}
