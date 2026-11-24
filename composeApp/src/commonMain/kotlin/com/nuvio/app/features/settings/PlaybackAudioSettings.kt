package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.player.AndroidPlaybackEngine
import com.nuvio.app.features.player.AudioLanguageOption
import com.nuvio.app.features.player.AudioOutputChannels
import com.nuvio.app.features.player.AvailableLanguageOptions
import com.nuvio.app.features.player.IosAudioOutputMode
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.player.effectiveDownmixEnabled
import com.nuvio.app.features.player.languageLabelForCode
import com.nuvio.app.isDesktop
import com.nuvio.app.isIos
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.features.player.PlayerSettingsUiState

@Composable
internal fun PlaybackAudioSettings(
    isTablet: Boolean,
    settings: PlayerSettingsUiState,
) {
    var showPreferredAudioDialog by remember { mutableStateOf(false) }
    var showSecondaryAudioDialog by remember { mutableStateOf(false) }
    var showIosAudioOutputDialog by remember { mutableStateOf(false) }
    var showDownmixChannelsDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 12.dp)) {
        SettingsSection(
            title = stringResource(Res.string.settings_playback_page_audio),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                val preferredAudioLabel = when (settings.preferredAudioLanguage) {
                    AudioLanguageOption.DEFAULT -> stringResource(Res.string.settings_playback_option_default)
                    AudioLanguageOption.DEVICE -> stringResource(Res.string.settings_playback_option_device_language)
                    AudioLanguageOption.ORIGINAL -> stringResource(Res.string.settings_playback_option_original)
                    else -> languageLabelForCode(settings.preferredAudioLanguage)
                }
                if (isDesktop) {
                    val originalHint = stringResource(Res.string.settings_playback_option_original_hint)
                    val preferredAudioOptions = listOf(
                        DesktopDropdownOption(
                            value = AudioLanguageOption.DEFAULT,
                            label = stringResource(Res.string.settings_playback_option_default),
                        ),
                        DesktopDropdownOption(
                            value = AudioLanguageOption.DEVICE,
                            label = stringResource(Res.string.settings_playback_option_device_language),
                        ),
                        DesktopDropdownOption(
                            value = AudioLanguageOption.ORIGINAL,
                            label = stringResource(Res.string.settings_playback_option_original),
                            description = originalHint,
                        ),
                    ) + AvailableLanguageOptions.map { option ->
                        DesktopDropdownOption(
                            value = option.code,
                            label = stringResource(option.labelRes),
                        )
                    }
                    DesktopSettingRow(
                        title = stringResource(Res.string.settings_playback_preferred_audio_language),
                        enabled = !settings.externalPlayerEnabled,
                    ) {
                        DesktopDropdownSelect(
                            label = preferredAudioLabel,
                            options = preferredAudioOptions,
                            selectedValue = settings.preferredAudioLanguage,
                            onSelected = PlayerSettingsRepository::setPreferredAudioLanguage,
                            enabled = !settings.externalPlayerEnabled,
                        )
                    }
                } else {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.settings_playback_preferred_audio_language),
                        description = preferredAudioLabel,
                        enabled = !settings.externalPlayerEnabled,
                        isTablet = isTablet,
                        valueLabel = preferredAudioLabel,
                        onClick = { showPreferredAudioDialog = true },
                    )
                }
                SettingsGroupDivider(isTablet = isTablet)
                val secondaryAudioLabel = languageLabelForCode(settings.secondaryPreferredAudioLanguage)
                if (isDesktop) {
                    val originalHint = stringResource(Res.string.settings_playback_option_original_hint)
                    val secondaryAudioOptions = listOf<DesktopDropdownOption<String?>>(
                        DesktopDropdownOption(
                            value = null,
                            label = stringResource(Res.string.settings_playback_option_none),
                        ),
                        DesktopDropdownOption(
                            value = AudioLanguageOption.ORIGINAL,
                            label = stringResource(Res.string.settings_playback_option_original),
                            description = originalHint,
                        ),
                    ) + AvailableLanguageOptions.map { option ->
                        DesktopDropdownOption<String?>(
                            value = option.code,
                            label = stringResource(option.labelRes),
                        )
                    }
                    DesktopSettingRow(
                        title = stringResource(Res.string.settings_playback_secondary_audio_language),
                        enabled = !settings.externalPlayerEnabled,
                    ) {
                        DesktopDropdownSelect(
                            label = secondaryAudioLabel,
                            options = secondaryAudioOptions,
                            selectedValue = settings.secondaryPreferredAudioLanguage,
                            onSelected = PlayerSettingsRepository::setSecondaryPreferredAudioLanguage,
                            enabled = !settings.externalPlayerEnabled,
                        )
                    }
                } else {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.settings_playback_secondary_audio_language),
                        description = secondaryAudioLabel,
                        enabled = !settings.externalPlayerEnabled,
                        isTablet = isTablet,
                        valueLabel = secondaryAudioLabel,
                        onClick = { showSecondaryAudioDialog = true },
                    )
                }
                if (!isIos && !isDesktop) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_playback_remember_audio_delay),
                        description = stringResource(Res.string.settings_playback_remember_audio_delay_description),
                        checked = settings.rememberAudioDelayPerDevice,
                        enabled = !settings.externalPlayerEnabled,
                        isTablet = isTablet,
                        onCheckedChange = PlayerSettingsRepository::setRememberAudioDelayPerDevice,
                    )
                }
            }
        }

        if (!isIos) {
            // The downmix runs in the app's FFmpeg decoder, which ExoPlayer only prefers with that decoder priority.
            val downmixAvailable = !settings.externalPlayerEnabled &&
                (isDesktop || (settings.androidPlaybackEngine != AndroidPlaybackEngine.Libmpv && settings.decoderPriority == 2))
            SettingsSection(
                title = stringResource(Res.string.settings_playback_downmix_section),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_playback_downmix),
                        description = stringResource(Res.string.settings_playback_downmix_description),
                        checked = settings.effectiveDownmixEnabled,
                        enabled = downmixAvailable,
                        isTablet = isTablet,
                        onCheckedChange = PlayerSettingsRepository::setDownmixEnabled,
                    )
                    if (settings.effectiveDownmixEnabled) {
                        SettingsGroupDivider(isTablet = isTablet)
                        if (isDesktop) {
                            DesktopSettingRow(
                                title = stringResource(Res.string.settings_playback_downmix_channels),
                                enabled = downmixAvailable,
                            ) {
                                DesktopDropdownSelect(
                                    label = settings.audioOutputChannels.settingValue,
                                    options = AudioOutputChannels.entries.map { channel ->
                                        DesktopDropdownOption(
                                            value = channel,
                                            label = channel.settingValue,
                                        )
                                    },
                                    selectedValue = settings.audioOutputChannels,
                                    onSelected = PlayerSettingsRepository::setAudioOutputChannels,
                                    enabled = downmixAvailable,
                                )
                            }
                        } else {
                            SettingsNavigationRow(
                                title = stringResource(Res.string.settings_playback_downmix_channels),
                                description = settings.audioOutputChannels.settingValue,
                                enabled = downmixAvailable,
                                isTablet = isTablet,
                                valueLabel = settings.audioOutputChannels.settingValue,
                                onClick = { showDownmixChannelsDialog = true },
                            )
                        }
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsSwitchRow(
                            title = stringResource(Res.string.settings_playback_downmix_maintain_volume),
                            description = stringResource(Res.string.settings_playback_downmix_maintain_volume_description),
                            checked = settings.maintainOriginalAudioOnDownmix,
                            enabled = downmixAvailable,
                            isTablet = isTablet,
                            onCheckedChange = PlayerSettingsRepository::setMaintainOriginalAudioOnDownmix,
                        )
                    }
                }
            }
        }

        if (isIos) {
            SettingsSection(
                title = stringResource(Res.string.settings_playback_ios_audio_output_section),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.settings_playback_ios_audio_output),
                        description = settings.iosAudioOutputMode.label,
                        isTablet = isTablet,
                        onClick = { showIosAudioOutputDialog = true },
                    )
                }
            }
        }
    }

    if (!isDesktop && showDownmixChannelsDialog) {
        SettingsChoiceDialog(
            title = stringResource(Res.string.settings_playback_downmix_channels),
            options = AudioOutputChannels.entries,
            selected = settings.audioOutputChannels,
            label = { it.settingValue },
            onSelect = {
                PlayerSettingsRepository.setAudioOutputChannels(it)
                showDownmixChannelsDialog = false
            },
            onDismiss = { showDownmixChannelsDialog = false },
        )
    }

    if (!isDesktop && showPreferredAudioDialog) {
        val originalHint = stringResource(Res.string.settings_playback_option_original_hint)
        LanguageSelectionDialog(
            title = stringResource(Res.string.settings_playback_preferred_audio_language),
            options = listOf(
                LanguageSelectionOption(AudioLanguageOption.DEFAULT, stringResource(Res.string.settings_playback_option_default)),
                LanguageSelectionOption(AudioLanguageOption.DEVICE, stringResource(Res.string.settings_playback_option_device_language)),
                LanguageSelectionOption(AudioLanguageOption.ORIGINAL, stringResource(Res.string.settings_playback_option_original), description = originalHint),
            ) + AvailableLanguageOptions.map { option ->
                LanguageSelectionOption(option.code, stringResource(option.labelRes))
            },
            selectedValue = settings.preferredAudioLanguage,
            onSelect = { value ->
                PlayerSettingsRepository.setPreferredAudioLanguage(value ?: AudioLanguageOption.DEVICE)
                showPreferredAudioDialog = false
            },
            onDismiss = { showPreferredAudioDialog = false },
        )
    }

    if (!isDesktop && showSecondaryAudioDialog) {
        val originalHint = stringResource(Res.string.settings_playback_option_original_hint)
        LanguageSelectionDialog(
            title = stringResource(Res.string.settings_playback_secondary_audio_language),
            options = listOf(
                LanguageSelectionOption(null, stringResource(Res.string.settings_playback_option_none)),
                LanguageSelectionOption(AudioLanguageOption.ORIGINAL, stringResource(Res.string.settings_playback_option_original), description = originalHint),
            ) + AvailableLanguageOptions.map { option ->
                LanguageSelectionOption(option.code, stringResource(option.labelRes))
            },
            selectedValue = settings.secondaryPreferredAudioLanguage,
            onSelect = { value ->
                PlayerSettingsRepository.setSecondaryPreferredAudioLanguage(value)
                showSecondaryAudioDialog = false
            },
            onDismiss = { showSecondaryAudioDialog = false },
        )
    }

    if (showIosAudioOutputDialog) {
        SettingsChoiceDialog(
            title = stringResource(Res.string.settings_playback_ios_audio_output_dialog),
            options = IosAudioOutputMode.selectableEntries,
            selected = settings.iosAudioOutputMode,
            label = { it.label },
            description = {
                when (it) {
                    IosAudioOutputMode.Auto -> stringResource(Res.string.settings_playback_ios_audio_output_auto_desc)
                    IosAudioOutputMode.AvFoundation -> stringResource(Res.string.settings_playback_ios_audio_output_avfoundation_desc)
                    IosAudioOutputMode.AudioUnit -> stringResource(Res.string.settings_playback_ios_audio_output_audiounit_desc)
                }
            },
            onSelect = {
                PlayerSettingsRepository.setIosAudioOutputMode(it)
                showIosAudioOutputDialog = false
            },
            onDismiss = { showIosAudioOutputDialog = false },
        )
    }
}
