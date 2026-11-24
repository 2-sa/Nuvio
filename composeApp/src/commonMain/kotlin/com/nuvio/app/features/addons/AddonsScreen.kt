package com.nuvio.app.features.addons

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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.isDesktop
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddonsScreen(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    val addInputFocusRequester = remember { FocusRequester() }
    val usePersonalMediaCopy = AppFeaturePolicy.personalMediaAddonCopyEnabled

    NuvioScreen(modifier = modifier) {
        stickyHeader {
            Column(modifier = Modifier.fillMaxWidth()) {
                NuvioScreenHeader(
                    title = title ?: stringResource(Res.string.addon_title),
                    onBack = onBack,
                ) {
                    if (isDesktop) {
                        HeaderAddAddonCtaPill(
                            label = stringResource(
                                if (usePersonalMediaCopy) {
                                    Res.string.addons_appstore_install_button
                                } else {
                                    Res.string.addons_install_button
                                },
                            ),
                            onClick = { runCatching { addInputFocusRequester.requestFocus() } },
                        )
                    }
                }
                if (isDesktop) {
                    HorizontalDivider(
                        thickness = NuvioTokens.Border.thin,
                        color = tokens.colors.borderSubtle,
                    )
                }
            }
        }
        item {
            AddonsSettingsPageContent(addInputFocusRequester = addInputFocusRequester)
        }
    }
}

@Composable
internal fun AddonsSettingsPageContent(
    modifier: Modifier = Modifier,
    addInputFocusRequester: FocusRequester? = null,
) {
    LaunchedEffect(Unit) {
        AddonRepository.initialize()
    }

    val uiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val pendingTrackingConsent by AddonRepository.pendingTrackingConsent.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    var addonUrl by rememberSaveable { mutableStateOf("") }
    var formMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var installModalState by remember { mutableStateOf<AddonInstallModalState?>(null) }
    var addonPendingDeletionUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val enterAddonUrlMessage = stringResource(Res.string.addons_error_enter_url)
    val usePersonalMediaCopy = AppFeaturePolicy.personalMediaAddonCopyEnabled

    val overview = remember(uiState.addons) { uiState.addons.toOverview() }
    val isInstalling = installModalState?.isBusy == true
    val onAddClick: () -> Unit = {
        val requestedUrl = addonUrl.trim()
        if (requestedUrl.isBlank()) {
            formMessage = enterAddonUrlMessage
        } else {
            formMessage = null
            installModalState = AddonInstallModalState.Checking
            coroutineScope.launch {
                val result = AddonRepository.addAddon(requestedUrl)
                installModalState = when (result) {
                    is AddAddonResult.Success -> {
                        addonUrl = ""
                        AddonInstallModalState.Success(result.manifest.name)
                    }

                    is AddAddonResult.TrackingConsentRequired -> {
                        AddonRepository.requestTrackingConsent(result.manifest)
                        null
                    }

                    is AddAddonResult.Error -> {
                        AddonInstallModalState.Error(result.message)
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (isDesktop) {
            val isTwoColumnDesktop = maxWidth >= 680.dp
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DesktopAddonStatsStrip(overview = overview)

                DesktopAddonInstallCommandBar(
                    addonUrl = addonUrl,
                    formMessage = formMessage,
                    isInstalling = isInstalling,
                    usePersonalMediaCopy = usePersonalMediaCopy,
                    focusRequester = addInputFocusRequester,
                    onAddonUrlChange = {
                        addonUrl = it
                        formMessage = null
                    },
                    onAddClick = onAddClick,
                )

                SectionHeader(stringResource(Res.string.addons_section_installed))
                if (uiState.addons.isEmpty()) {
                    EmptyStateCard(usePersonalMediaCopy = usePersonalMediaCopy)
                } else if (isTwoColumnDesktop) {
                    val lastIndex = uiState.addons.lastIndex
                    val indexedAddons = uiState.addons.mapIndexed { index, addon -> index to addon }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        indexedAddons.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                pair.forEach { (index, addon) ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        InstalledAddonCardItem(
                                            addon = addon,
                                            index = index,
                                            lastIndex = lastIndex,
                                            compactDesktop = true,
                                            onRequestDelete = { addonPendingDeletionUrl = it },
                                            onOpenConfigure = { url -> runCatching { uriHandler.openUri(url) } },
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
                    val lastIndex = uiState.addons.lastIndex
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        uiState.addons.forEachIndexed { index, addon ->
                            InstalledAddonCardItem(
                                addon = addon,
                                index = index,
                                lastIndex = lastIndex,
                                compactDesktop = true,
                                onRequestDelete = { addonPendingDeletionUrl = it },
                                onOpenConfigure = { url -> runCatching { uriHandler.openUri(url) } },
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionHeader(stringResource(Res.string.addons_section_overview))
                OverviewCard(overview = overview)

                SectionHeader(stringResource(Res.string.addons_section_add_addon))
                AddAddonCard(
                    addonUrl = addonUrl,
                    formMessage = formMessage,
                    usePersonalMediaCopy = usePersonalMediaCopy,
                    focusRequester = addInputFocusRequester,
                    onAddonUrlChange = {
                        addonUrl = it
                        formMessage = null
                    },
                    onAddClick = onAddClick,
                )

                SectionHeader(stringResource(Res.string.addons_section_installed))
                if (uiState.addons.isEmpty()) {
                    EmptyStateCard(usePersonalMediaCopy = usePersonalMediaCopy)
                } else {
                    val lastIndex = uiState.addons.lastIndex
                    uiState.addons.forEachIndexed { index, addon ->
                        InstalledAddonCardItem(
                            addon = addon,
                            index = index,
                            lastIndex = lastIndex,
                            compactDesktop = false,
                            onRequestDelete = { addonPendingDeletionUrl = it },
                            onOpenConfigure = { url -> runCatching { uriHandler.openUri(url) } },
                        )
                    }
                }
            }
        }
    }

    val pendingDeletionUrl = addonPendingDeletionUrl
    if (pendingDeletionUrl != null) {
        NuvioStatusModal(
            title = stringResource(Res.string.addons_delete_confirm_title),
            message = stringResource(Res.string.action_delete_confirm_message),
            isVisible = true,
            destructive = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                AddonRepository.removeAddon(pendingDeletionUrl)
                addonPendingDeletionUrl = null
            },
            onDismiss = { addonPendingDeletionUrl = null },
        )
    }

    val trackingConsentManifest = pendingTrackingConsent
    if (trackingConsentManifest != null) {
        TrackingAddonConsentDialog(
            manifest = trackingConsentManifest,
            onInstall = {
                installModalState = AddonInstallModalState.Checking
                coroutineScope.launch {
                    installModalState = when (val result = AddonRepository.installWithTrackingConsent(trackingConsentManifest)) {
                        is AddAddonResult.Success -> {
                            addonUrl = ""
                            AddonInstallModalState.Success(result.manifest.name)
                        }

                        is AddAddonResult.TrackingConsentRequired -> null

                        is AddAddonResult.Error -> AddonInstallModalState.Error(result.message)
                    }
                }
            },
            onDismiss = AddonRepository::dismissTrackingConsent,
        )
    }

    val modalState = installModalState
    if (modalState != null) {
        val modalTitle = when (modalState) {
            AddonInstallModalState.Checking -> stringResource(Res.string.addons_modal_checking_title)
            is AddonInstallModalState.Success -> stringResource(Res.string.addons_modal_success_title)
            is AddonInstallModalState.Error -> stringResource(Res.string.addons_modal_failure_title)
        }
        val modalMessage = when (modalState) {
            AddonInstallModalState.Checking -> stringResource(Res.string.addons_modal_checking_message)
            is AddonInstallModalState.Success -> stringResource(
                Res.string.addons_modal_success_message,
                modalState.addonName,
            )
            is AddonInstallModalState.Error -> modalState.reason
        }
        val modalConfirmText = when (modalState) {
            AddonInstallModalState.Checking -> stringResource(Res.string.addon_installing)
            is AddonInstallModalState.Success -> stringResource(Res.string.action_done)
            is AddonInstallModalState.Error -> stringResource(Res.string.action_close)
        }
        NuvioStatusModal(
            title = modalTitle,
            message = modalMessage,
            isVisible = true,
            isBusy = modalState.isBusy,
            confirmText = modalConfirmText,
            onConfirm = {
                if (!modalState.isBusy) {
                    installModalState = null
                }
            },
        )
    }
}

@Composable
private fun InstalledAddonCardItem(
    addon: ManagedAddon,
    index: Int,
    lastIndex: Int,
    compactDesktop: Boolean = false,
    onRequestDelete: (String) -> Unit,
    onOpenConfigure: (String) -> Unit,
) {
    val manifest = addon.manifest
    val behaviorHints = manifest?.behaviorHints
    val showConfigureAction = behaviorHints?.configurable == true || behaviorHints?.configurationRequired == true
    val configureUrl = addon.manifestUrl.toConfigureUrl()
    val onMoveUpClick = if (index > 0) {
        { AddonRepository.moveAddon(index, index - 1) }
    } else {
        null
    }
    val onMoveDownClick = if (index < lastIndex) {
        { AddonRepository.moveAddon(index, index + 1) }
    } else {
        null
    }
    val onRefreshClick = {
        AddonRepository.refreshAddon(
            manifestUrl = addon.manifestUrl,
            forceRefresh = true,
        )
    }
    val onEnabledChange: (Boolean) -> Unit = { enabled ->
        AddonRepository.setAddonEnabled(addon.manifestUrl, enabled)
    }
    val onConfigureClick = if (showConfigureAction && configureUrl.isNotBlank()) {
        { onOpenConfigure(configureUrl) }
    } else {
        null
    }
    val onDeleteClick = { onRequestDelete(addon.manifestUrl) }

    if (compactDesktop) {
        DesktopStudioAddonCard(
            addon = addon,
            onMoveUpClick = onMoveUpClick,
            onMoveDownClick = onMoveDownClick,
            onRefreshClick = onRefreshClick,
            onEnabledChange = onEnabledChange,
            onConfigureClick = onConfigureClick,
            onDeleteClick = onDeleteClick,
        )
    } else {
        InstalledAddonCard(
            addon = addon,
            onMoveUpClick = onMoveUpClick,
            onMoveDownClick = onMoveDownClick,
            onRefreshClick = onRefreshClick,
            onEnabledChange = onEnabledChange,
            onConfigureClick = onConfigureClick,
            onDeleteClick = onDeleteClick,
        )
    }
}

@Composable
private fun DesktopAddonStatsStrip(
    overview: AddonOverview,
) {
    val tokens = MaterialTheme.nuvio
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DesktopAddonStatPill(
            text = pluralStringResource(Res.plurals.addons_stats_installed, overview.totalAddons, overview.totalAddons),
        )
        DesktopAddonStatPill(
            text = pluralStringResource(Res.plurals.addons_stats_active, overview.activeAddons, overview.activeAddons),
            dotColor = if (overview.activeAddons > 0) Color(0xFF34D399) else tokens.colors.textMuted,
            accentHighlight = overview.activeAddons > 0,
        )
        DesktopAddonStatPill(
            text = pluralStringResource(Res.plurals.addons_stats_catalogs, overview.totalCatalogs, overview.totalCatalogs),
        )
    }
}

@Composable
private fun DesktopAddonStatPill(
    text: String,
    dotColor: Color? = null,
    accentHighlight: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    val pillShape = RoundedCornerShape(999.dp)
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
            .clip(pillShape)
            .background(bgColor)
            .border(NuvioTokens.Border.thin, borderColor, pillShape)
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

@Suppress("DEPRECATION")
@Composable
private fun DesktopAddonInstallCommandBar(
    addonUrl: String,
    formMessage: String?,
    isInstalling: Boolean,
    usePersonalMediaCopy: Boolean,
    focusRequester: FocusRequester?,
    onAddonUrlChange: (String) -> Unit,
    onAddClick: () -> Unit,
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
    val ctaEnabled = addonUrl.isNotBlank() && !isInstalling

    val barBorderColor by animateColorAsState(
        targetValue = when {
            isInputFocused -> tokens.colors.accent.copy(alpha = 0.72f)
            isBarHovered -> Color.White.copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.09f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "addonInstallBarBorder",
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
                if (addonUrl.isEmpty()) {
                    Text(
                        text = stringResource(
                            if (usePersonalMediaCopy) {
                                Res.string.addons_appstore_input_placeholder
                            } else {
                                Res.string.addons_input_placeholder
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = addonUrl,
                    onValueChange = onAddonUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .onFocusChanged { isInputFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (
                                event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                if (!isInstalling && addonUrl.isNotBlank()) {
                                    onAddClick()
                                }
                                true
                            } else {
                                false
                            }
                        },
                    singleLine = true,
                    enabled = !isInstalling,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        color = tokens.colors.textPrimary,
                        textDirection = TextDirection.Ltr,
                    ),
                    cursorBrush = SolidColor(tokens.colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isInstalling && addonUrl.isNotBlank()) {
                                onAddClick()
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
                        enabled = !isInstalling,
                        interactionSource = pasteInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                            if (pasted.isNotEmpty()) {
                                onAddonUrlChange(pasted)
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
                    .alpha(if (ctaEnabled || isInstalling) 1f else tokens.opacity.disabled)
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
                        onClick = onAddClick,
                    )
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isInstalling) {
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
                    text = stringResource(
                        when {
                            isInstalling -> Res.string.addon_installing
                            usePersonalMediaCopy -> Res.string.addons_appstore_install_button
                            else -> Res.string.addons_install_button
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = tokens.colors.onAccent,
                    maxLines = 1,
                )
            }
        }

        if (usePersonalMediaCopy) {
            Text(
                text = stringResource(Res.string.addons_appstore_add_description),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = tokens.colors.textMuted,
            )
        }
        formMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = tokens.colors.danger,
            )
        }
    }
}

@Composable
private fun DesktopStudioAddonCard(
    addon: ManagedAddon,
    onMoveUpClick: (() -> Unit)?,
    onMoveDownClick: (() -> Unit)?,
    onRefreshClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onConfigureClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val manifest = addon.manifest
    val cardShape = RoundedCornerShape(14.dp)
    val iconShape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isHovered) {
            tokens.colors.accent.copy(alpha = 0.40f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktopAddonCardBorder",
    )

    val statusDotColor: Color
    val statusText: String
    val statusTextColor: Color
    when {
        !addon.enabled -> {
            statusDotColor = tokens.colors.textMuted
            statusText = stringResource(Res.string.addons_badge_disabled)
            statusTextColor = tokens.colors.textMuted
        }
        addon.isRefreshing -> {
            statusDotColor = tokens.colors.accent
            statusText = stringResource(Res.string.addons_badge_refreshing)
            statusTextColor = tokens.colors.accent
        }
        manifest != null -> {
            statusDotColor = Color(0xFF34D399)
            statusText = stringResource(Res.string.addons_badge_active)
            statusTextColor = tokens.colors.textSecondary
        }
        else -> {
            statusDotColor = tokens.colors.danger
            statusText = stringResource(Res.string.addons_badge_unavailable)
            statusTextColor = tokens.colors.danger
        }
    }

    val capabilityBadges = remember(manifest?.resources, manifest?.catalogs) {
        extractAddonCapabilityBadges(
            resources = manifest?.resources?.map { it.name }.orEmpty(),
            catalogCount = manifest?.catalogs?.size ?: 0,
        )
    }
    val contentTypes = remember(manifest?.types) {
        normalizeAddonContentTypes(manifest?.types.orEmpty())
    }
    val localizedTypesLine = contentTypes.map { typeKind ->
        when (typeKind) {
            AddonContentTypeKind.MOVIE -> stringResource(Res.string.addons_type_movie)
            AddonContentTypeKind.SERIES -> stringResource(Res.string.addons_type_series)
            AddonContentTypeKind.ANIME -> stringResource(Res.string.addons_type_anime)
            AddonContentTypeKind.CHANNEL -> stringResource(Res.string.addons_type_channel)
            AddonContentTypeKind.OTHER -> stringResource(Res.string.addons_type_other)
        }
    }.joinToString(separator = " · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, borderColor, cardShape)
            .hoverable(interactionSource)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Row 1: Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(iconShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(NuvioTokens.Border.thin, Color.White.copy(alpha = 0.09f), iconShape),
                contentAlignment = Alignment.Center,
            ) {
                val logoUrl = manifest?.logoUrl
                if (!logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Extension,
                        contentDescription = null,
                        tint = if (manifest != null) tokens.colors.accent else tokens.colors.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = addon.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textDirection = if (isPrimarilyLatinText(addon.displayTitle)) {
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
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusDotColor),
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = statusTextColor,
                        maxLines = 1,
                    )
                    manifest?.version?.takeIf { it.isNotBlank() }?.let { version ->
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = tokens.colors.textMuted,
                        )
                        Text(
                            text = "v$version",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                textDirection = TextDirection.Ltr,
                            ),
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Switch(
                checked = addon.enabled,
                onCheckedChange = onEnabledChange,
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

        // Row 2: Body
        when {
            addon.isRefreshing && manifest == null -> {
                Text(
                    text = stringResource(Res.string.addons_loading_manifest_details),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = tokens.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            addon.errorMessage != null && manifest == null -> {
                Text(
                    text = addon.errorMessage,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = tokens.colors.danger,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            manifest != null -> {
                manifest.description.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            textDirection = if (isPrimarilyLatinText(description)) {
                                TextDirection.Ltr
                            } else {
                                TextDirection.Content
                            },
                        ),
                        color = tokens.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                addon.errorMessage?.let { staleError ->
                    Text(
                        text = staleError,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = tokens.colors.danger,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Hairline divider
        HorizontalDivider(
            thickness = NuvioTokens.Border.hairline,
            color = Color.White.copy(alpha = 0.06f),
        )

        // Row 3: Unified Footer
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
                capabilityBadges.forEach { badge ->
                    val label = when (badge.kind) {
                        AddonCapabilityKind.STREAM -> stringResource(Res.string.addons_cap_stream)
                        AddonCapabilityKind.CATALOGS -> {
                            val count = badge.count ?: 0
                            pluralStringResource(Res.plurals.addons_stats_catalogs, count, count)
                        }
                        AddonCapabilityKind.META -> stringResource(Res.string.addons_cap_meta)
                        AddonCapabilityKind.SUBTITLES -> stringResource(Res.string.addons_cap_subtitles)
                    }
                    DesktopCapabilityPill(
                        text = label,
                        accent = badge.kind == AddonCapabilityKind.STREAM,
                    )
                }
                if (localizedTypesLine.isNotEmpty()) {
                    Text(
                        text = localizedTypesLine,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onMoveUpClick?.let { onMoveUp ->
                    DesktopCompactActionButton(
                        icon = Icons.Rounded.ArrowUpward,
                        contentDescription = stringResource(Res.string.addons_move_up),
                        tint = tokens.colors.textSecondary,
                        onClick = onMoveUp,
                    )
                }
                onMoveDownClick?.let { onMoveDown ->
                    DesktopCompactActionButton(
                        icon = Icons.Rounded.ArrowDownward,
                        contentDescription = stringResource(Res.string.addons_move_down),
                        tint = tokens.colors.textSecondary,
                        onClick = onMoveDown,
                    )
                }
                DesktopCompactActionButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = stringResource(Res.string.addons_refresh),
                    tint = tokens.colors.textSecondary,
                    hoverTint = tokens.colors.accent,
                    onClick = onRefreshClick,
                )
                onConfigureClick?.let { onConfigure ->
                    DesktopCompactActionButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = stringResource(Res.string.addons_configure),
                        tint = tokens.colors.textSecondary,
                        hoverTint = tokens.colors.textPrimary,
                        onClick = onConfigure,
                    )
                }
                DesktopCompactActionButton(
                    icon = Icons.Rounded.Delete,
                    contentDescription = stringResource(Res.string.addons_delete),
                    tint = Color(0xFFFB7185).copy(alpha = 0.85f),
                    hoverTint = Color(0xFFFB7185),
                    destructive = true,
                    onClick = onDeleteClick,
                )
            }
        }
    }
}

@Composable
private fun DesktopCapabilityPill(
    text: String,
    accent: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(6.dp)
    val bgColor = if (accent) tokens.colors.accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f)
    val borderColor = if (accent) tokens.colors.accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f)
    val textColor = if (accent) tokens.colors.accent else tokens.colors.textSecondary
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
private fun DesktopCompactActionButton(
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
        label = "desktopAddonActionBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isHovered && destructive -> Color(0xFFFB7185).copy(alpha = 0.45f)
            isHovered -> Color.White.copy(alpha = 0.18f)
            else -> Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktopAddonActionBorder",
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
private fun HeaderAddAddonCtaPill(
    label: String,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val pillShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(pillShape)
            .background(palette.accentBrush())
            .then(
                if (isHovered) {
                    Modifier.background(Color.White.copy(alpha = NuvioTokens.Opacity.hover))
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(pillShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = tokens.colors.onAccent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tokens.colors.onAccent,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    NuvioSectionLabel(text = text)
}

@Composable
private fun OverviewCard(overview: AddonOverview) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
            .padding(tokens.spacing.cardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverviewStat(
                value = overview.totalAddons.toString(),
                label = stringResource(Res.string.addons_overview_addons),
                modifier = Modifier.weight(1f),
            )
            VerticalSeparator()
            OverviewStat(
                value = overview.activeAddons.toString(),
                label = stringResource(Res.string.addons_overview_active),
                valueColor = tokens.colors.accent,
                modifier = Modifier.weight(1f),
            )
            VerticalSeparator()
            OverviewStat(
                value = overview.totalCatalogs.toString(),
                label = stringResource(Res.string.addons_overview_catalogs),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OverviewStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.nuvio.colors.textPrimary,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VerticalSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .width(1.dp)
            .height(52.dp)
            .background(MaterialTheme.nuvio.colors.borderSubtle),
    )
}

@Composable
private fun AddAddonCard(
    addonUrl: String,
    formMessage: String?,
    usePersonalMediaCopy: Boolean,
    focusRequester: FocusRequester?,
    onAddonUrlChange: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val cardShape = RoundedCornerShape(16.dp)
    val ctaShape = RoundedCornerShape(12.dp)
    val ctaInteraction = remember { MutableInteractionSource() }
    val isCtaHovered by ctaInteraction.collectIsHoveredAsState()
    val ctaEnabled = addonUrl.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
            .padding(tokens.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NuvioInputField(
            value = addonUrl,
            onValueChange = onAddonUrlChange,
            placeholder = stringResource(
                if (usePersonalMediaCopy) {
                    Res.string.addons_appstore_input_placeholder
                } else {
                    Res.string.addons_input_placeholder
                },
            ),
            modifier = if (focusRequester != null) {
                Modifier.focusRequester(focusRequester)
            } else {
                Modifier
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .alpha(if (ctaEnabled) 1f else tokens.opacity.disabled)
                .clip(ctaShape)
                .background(palette.accentBrush())
                .then(
                    if (isDesktop && ctaEnabled && isCtaHovered) {
                        Modifier.background(Color.White.copy(alpha = NuvioTokens.Opacity.hover))
                    } else {
                        Modifier
                    },
                )
                .nuvioFocusBorder(ctaShape)
                .then(
                    if (isDesktop && ctaEnabled) {
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
                    onClick = onAddClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = tokens.colors.onAccent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(
                        if (usePersonalMediaCopy) {
                            Res.string.addons_appstore_install_button
                        } else {
                            Res.string.addons_install_button
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.onAccent,
                )
            }
        }
        if (usePersonalMediaCopy) {
            Text(
                text = stringResource(Res.string.addons_appstore_add_description),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textSecondary,
            )
        }
        formMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.danger,
            )
        }
    }
}

private sealed interface AddonInstallModalState {
    val isBusy: Boolean

    data object Checking : AddonInstallModalState {
        override val isBusy: Boolean = true
    }

    data class Success(
        val addonName: String,
    ) : AddonInstallModalState {
        override val isBusy: Boolean = false
    }

    data class Error(
        val reason: String,
    ) : AddonInstallModalState {
        override val isBusy: Boolean = false
    }
}

@Composable
private fun EmptyStateCard(
    usePersonalMediaCopy: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
            .padding(tokens.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(
                if (usePersonalMediaCopy) {
                    Res.string.addons_appstore_empty_title
                } else {
                    Res.string.addons_empty_title
                },
            ),
            style = MaterialTheme.typography.titleLarge,
            color = tokens.colors.textPrimary,
        )
        Text(
            text = stringResource(
                if (usePersonalMediaCopy) {
                    Res.string.addons_appstore_empty_subtitle
                } else {
                    Res.string.addons_empty_subtitle
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textSecondary,
        )
    }
}

@Composable
private fun InstalledAddonCard(
    addon: ManagedAddon,
    onMoveUpClick: (() -> Unit)?,
    onMoveDownClick: (() -> Unit)?,
    onRefreshClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onConfigureClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val manifest = addon.manifest
    val cardShape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) tokens.colors.borderStrong else tokens.colors.borderSubtle,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "addonCardBorder",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, borderColor, cardShape)
            .then(
                if (isDesktop) Modifier.hoverable(interactionSource) else Modifier,
            )
            .padding(tokens.spacing.cardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            AddonIconBadge(
                imageUrl = manifest?.logoUrl,
                icon = Icons.Rounded.Extension,
                tint = if (manifest != null) tokens.colors.accent else tokens.colors.textMuted,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = addon.displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = tokens.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AddonStatusPill(
                        enabled = addon.enabled,
                        isRefreshing = addon.isRefreshing,
                        hasManifest = manifest != null,
                    )
                    manifest?.version?.takeIf { it.isNotBlank() }?.let { version ->
                        AddonMetaPill(
                            text = stringResource(Res.string.addons_version_format, version),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = addon.enabled,
                onCheckedChange = onEnabledChange,
                modifier = if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.colors.onAccent,
                    checkedTrackColor = tokens.colors.accent,
                    uncheckedThumbColor = tokens.colors.textMuted,
                    uncheckedTrackColor = tokens.colors.surfaceElevated,
                    uncheckedBorderColor = tokens.colors.borderStrong,
                ),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            onMoveUpClick?.let { onMoveUp ->
                AddonActionPillButton(
                    icon = Icons.Rounded.ArrowUpward,
                    contentDescription = stringResource(Res.string.addons_move_up),
                    tint = tokens.colors.textSecondary,
                    onClick = onMoveUp,
                )
            }
            onMoveDownClick?.let { onMoveDown ->
                AddonActionPillButton(
                    icon = Icons.Rounded.ArrowDownward,
                    contentDescription = stringResource(Res.string.addons_move_down),
                    tint = tokens.colors.textSecondary,
                    onClick = onMoveDown,
                )
            }
            AddonActionPillButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = stringResource(Res.string.addons_refresh),
                tint = tokens.colors.accent,
                onClick = onRefreshClick,
            )
            onConfigureClick?.let { onConfigure ->
                AddonActionPillButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = stringResource(Res.string.addons_configure),
                    tint = tokens.colors.textPrimary,
                    onClick = onConfigure,
                )
            }
            AddonActionPillButton(
                icon = Icons.Rounded.Delete,
                contentDescription = stringResource(Res.string.addons_delete),
                tint = tokens.colors.danger,
                destructive = true,
                onClick = onDeleteClick,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(
            thickness = NuvioTokens.Border.thin,
            color = tokens.colors.borderSubtle,
        )
        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            manifest?.let {
                AddonMetaPill(text = stringResource(Res.string.addons_badge_resources, it.resources.size))
                AddonMetaPill(text = stringResource(Res.string.addons_badge_catalogs, it.catalogs.size))
                if (it.behaviorHints.configurable) {
                    AddonMetaPill(text = stringResource(Res.string.addons_badge_configurable))
                }
            }
        }

        when {
            addon.isRefreshing -> {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.addons_loading_manifest_details),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textSecondary,
                )
            }

            addon.errorMessage != null && manifest == null -> {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = addon.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.danger,
                )
            }

            manifest != null -> {
                Spacer(modifier = Modifier.height(12.dp))
                manifest.description.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = manifestSummary(manifest),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                )

                addon.errorMessage?.let { staleError ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = staleError,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.danger,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddonStatusPill(
    enabled: Boolean,
    isRefreshing: Boolean,
    hasManifest: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    val statusText: String
    val dotColor: Color
    when {
        !enabled -> {
            statusText = stringResource(Res.string.addons_badge_disabled)
            dotColor = tokens.colors.textMuted
        }
        isRefreshing -> {
            statusText = stringResource(Res.string.addons_badge_refreshing)
            dotColor = tokens.colors.accent
        }
        hasManifest -> {
            statusText = stringResource(Res.string.addons_badge_active)
            dotColor = tokens.colors.success
        }
        else -> {
            statusText = stringResource(Res.string.addons_badge_unavailable)
            dotColor = tokens.colors.danger
        }
    }
    val pillShape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .clip(pillShape)
            .background(tokens.colors.surfaceElevated)
            .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, pillShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tokens.colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun AddonMetaPill(text: String) {
    val tokens = MaterialTheme.nuvio
    val pillShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .clip(pillShape)
            .background(tokens.colors.surfaceElevated)
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, pillShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = tokens.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddonActionPillButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val pillShape = RoundedCornerShape(10.dp)
    val bgColor by animateColorAsState(
        targetValue = when {
            isDesktop && isHovered && destructive -> tokens.colors.danger.copy(alpha = 0.14f)
            isDesktop && isHovered -> tokens.colors.surfacePopover
            else -> tokens.colors.surfaceElevated
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "addonActionPillBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isDesktop && isHovered && destructive -> tokens.colors.danger.copy(alpha = 0.6f)
            isDesktop && isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderDefault
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "addonActionPillBorder",
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(pillShape)
            .background(bgColor)
            .border(NuvioTokens.Border.thin, borderColor, pillShape)
            .nuvioFocusBorder(pillShape)
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
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun AddonIconBadge(
    imageUrl: String?,
    icon: ImageVector,
    tint: Color,
) {
    val tokens = MaterialTheme.nuvio
    val iconShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(iconShape)
            .background(tokens.colors.surfaceElevated)
            .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, iconShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun manifestSummary(manifest: AddonManifest): String {
    val resources = manifest.resources.joinToString(separator = ", ") { it.name }
    val types = manifest.types.joinToString(separator = " / ") { it.replaceFirstChar(Char::uppercase) }
    return buildString {
        append(types)
        append(" • ")
        append(resources)
        if (manifest.idPrefixes.isNotEmpty()) {
            append(" • ")
            append(stringResource(Res.string.addons_summary_id_rules, manifest.idPrefixes.size))
        }
        if (manifest.behaviorHints.p2p) {
            append(" • P2P")
        }
    }
}

private fun String.toConfigureUrl(): String {
    val base = substringBefore("?").trimEnd('/')
    return if (base.endsWith("/manifest.json")) {
        base.removeSuffix("/manifest.json") + "/configure"
    } else {
        "$base/configure"
    }
}
