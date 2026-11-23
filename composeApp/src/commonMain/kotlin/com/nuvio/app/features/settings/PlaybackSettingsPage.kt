package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.features.p2p.P2pSettingsRepository
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.isDesktop
import com.nuvio.app.isIos
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.playbackSettingsContent(
    isTablet: Boolean,
    page: SettingsPage,
    onPageChange: (SettingsPage) -> Unit,
) {
    item {
        val settings by remember {
            PlayerSettingsRepository.ensureLoaded()
            PlayerSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        when (page) {
            SettingsPage.Playback -> {
                val sections = visiblePlaybackSections(isIos, settings.androidPlaybackEngine, P2pSettingsRepository.isVisible)
                if (isDesktop) {
                    SettingsBentoGrid(
                        items = sections.map { section ->
                            SettingsBentoItem(
                                id = section.name,
                                title = stringResource(section.page.titleRes),
                                description = stringResource(section.descriptionRes),
                                icon = section.page.icon,
                                onClick = { onPageChange(section.page) },
                            )
                        },
                        isTablet = isTablet,
                    )
                } else {
                    SettingsGroup(isTablet = isTablet) {
                        sections.forEachIndexed { index, section ->
                            if (index > 0) SettingsGroupDivider(isTablet = isTablet)
                            SettingsNavigationRow(
                                title = stringResource(section.page.titleRes),
                                description = stringResource(section.descriptionRes),
                                isTablet = isTablet,
                                onClick = { onPageChange(section.page) },
                            )
                        }
                    }
                }
            }
            SettingsPage.PlaybackPlayer -> PlaybackPlayerSettings(isTablet, settings)
            SettingsPage.PlaybackStreamSelection -> PlaybackStreamSettings(isTablet, settings)
            SettingsPage.PlaybackUpNext -> PlaybackUpNextSettings(isTablet, settings)
            SettingsPage.PlaybackSkipSegments -> PlaybackSkipSettings(isTablet, settings)
            SettingsPage.PlaybackInterface -> PlaybackInterfaceSettings(isTablet, settings)
            SettingsPage.PlaybackAudio -> PlaybackAudioSettings(isTablet, settings)
            SettingsPage.PlaybackSubtitles -> PlaybackSubtitleSettings(isTablet, settings)
            SettingsPage.PlaybackVideo -> PlaybackVideoSettings(isTablet, settings)
            SettingsPage.PlaybackBuffer -> if (!isIos) PlaybackBufferSettings(isTablet, settings)
            SettingsPage.PlaybackP2p -> if (P2pSettingsRepository.isVisible) PlaybackP2pSettings(isTablet)
            else -> Unit
        }
    }
}

internal fun LazyListScope.desktopPlaybackTabContent(
    tab: DesktopSettingsWorkspaceTab,
    isTablet: Boolean,
) {
    when (tab) {
        DesktopSettingsWorkspaceTab.PlayerAndVideo -> {
            item(key = "desktop-playback-player-video") {
                val settings by remember {
                    PlayerSettingsRepository.ensureLoaded()
                    PlayerSettingsRepository.uiState
                }.collectAsStateWithLifecycle()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s20),
                ) {
                    PlaybackPlayerSettings(isTablet, settings)
                    PlaybackVideoSettings(isTablet, settings)
                }
            }
        }
        DesktopSettingsWorkspaceTab.AudioAndSubtitles -> {
            item(key = "desktop-playback-audio-subtitles") {
                val settings by remember {
                    PlayerSettingsRepository.ensureLoaded()
                    PlayerSettingsRepository.uiState
                }.collectAsStateWithLifecycle()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s20),
                ) {
                    PlaybackAudioSettings(isTablet, settings)
                    PlaybackSubtitleSettings(isTablet, settings)
                }
            }
        }
        DesktopSettingsWorkspaceTab.StreamsAndBadges -> {
            item(key = "desktop-playback-stream-selection") {
                val settings by remember {
                    PlayerSettingsRepository.ensureLoaded()
                    PlayerSettingsRepository.uiState
                }.collectAsStateWithLifecycle()
                PlaybackStreamSettings(isTablet, settings)
            }
            streamsSettingsContent(isTablet = isTablet)
        }
        DesktopSettingsWorkspaceTab.UpNextAndSkip -> {
            item(key = "desktop-playback-upnext-skip") {
                val settings by remember {
                    PlayerSettingsRepository.ensureLoaded()
                    PlayerSettingsRepository.uiState
                }.collectAsStateWithLifecycle()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s20),
                ) {
                    PlaybackUpNextSettings(isTablet, settings)
                    PlaybackSkipSettings(isTablet, settings)
                }
            }
        }
        DesktopSettingsWorkspaceTab.InterfaceAndNetwork -> {
            item(key = "desktop-playback-interface-network") {
                val settings by remember {
                    PlayerSettingsRepository.ensureLoaded()
                    PlayerSettingsRepository.uiState
                }.collectAsStateWithLifecycle()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s20),
                ) {
                    PlaybackInterfaceSettings(isTablet, settings)
                    if (!isIos && !isDesktop) {
                        PlaybackBufferSettings(isTablet, settings)
                    }
                    if (P2pSettingsRepository.isVisible) {
                        PlaybackP2pSettings(isTablet)
                    }
                }
            }
        }
        else -> Unit
    }
}
