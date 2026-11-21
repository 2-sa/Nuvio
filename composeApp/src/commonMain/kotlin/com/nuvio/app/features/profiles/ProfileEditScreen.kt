package com.nuvio.app.features.profiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.membership.CosmeticEntitlement
import com.nuvio.app.features.membership.MemberAccessRepository
import com.nuvio.app.features.membership.ProfileBackgroundRepository
import com.nuvio.app.isDesktop
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileEditScreen(
    profile: NuvioProfile? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNew = profile == null
    val scope = rememberCoroutineScope()
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val currentProfile = remember(profile?.profileIndex, profileState.profiles, profile) {
        profile?.let { snapshot ->
            profileState.profiles.find { it.profileIndex == snapshot.profileIndex } ?: snapshot
        }
    }
    val fallbackColorHex = currentProfile?.avatarColorHex ?: PROFILE_COLORS.first()

    var name by rememberSaveable { mutableStateOf(currentProfile?.name ?: "") }
    var selectedAvatarId by rememberSaveable { mutableStateOf(currentProfile?.avatarId) }
    var avatarUrl by rememberSaveable { mutableStateOf(currentProfile?.avatarUrl.orEmpty()) }
    var selectedBackgroundId by rememberSaveable { mutableStateOf(currentProfile?.profileBackgroundId) }
    var selectedBackgroundUrl by rememberSaveable { mutableStateOf(currentProfile?.profileBackgroundUrl) }
    var usesPrimaryAddons by rememberSaveable { mutableStateOf(currentProfile?.usesPrimaryAddons ?: false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showPinClear by remember { mutableStateOf(false) }
    val memberAccess by remember {
        MemberAccessRepository.ensureStarted()
        MemberAccessRepository.access
    }.collectAsStateWithLifecycle()
    val backgroundCatalog by ProfileBackgroundRepository.catalog.collectAsStateWithLifecycle()
    val canChooseBackground = !isNew && memberAccess.entitlements.includes(CosmeticEntitlement.PROFILE_BACKGROUNDS)

    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        AvatarRepository.refreshAvatars()
    }
    LaunchedEffect(canChooseBackground) {
        if (canChooseBackground) ProfileBackgroundRepository.preloadLandscapeImages()
    }
    LaunchedEffect(isNew, avatars, selectedAvatarId, avatarUrl) {
        if (isNew && avatarUrl.isBlank() && selectedAvatarId == null && avatars.isNotEmpty()) {
            selectedAvatarId = avatars.first().id
        }
    }

    val customAvatarUrl = remember(avatarUrl) { normalizedAvatarUrl(avatarUrl) }
    val avatarUrlIsInvalid = avatarUrl.isNotBlank() && customAvatarUrl == null
    val selectedAvatarItem = remember(selectedAvatarId, avatars) {
        selectedAvatarId?.let { id -> avatars.find { it.id == id } }
    }
    val visibleAvatarItem = if (customAvatarUrl == null) selectedAvatarItem else null
    val previewAccent = remember(visibleAvatarItem, fallbackColorHex) {
        parseHexColor(visibleAvatarItem?.bgColor ?: fallbackColorHex)
    }

    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val basePadding = if (isDesktop) NuvioTokens.Space.s24 else tokens.spacing.screenHorizontal
        val maxFormWidth = 560.dp
        val horizontalPadding = if (isDesktop && maxWidth > maxFormWidth + basePadding * 2) {
            (maxWidth - maxFormWidth) / 2
        } else {
            basePadding
        }

        NuvioScreen(
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = horizontalPadding,
        ) {
            stickyHeader {
                NuvioScreenHeader(
                    title = if (isNew) {
                        stringResource(Res.string.profile_edit_add_title)
                    } else {
                        stringResource(Res.string.profile_edit_edit_title)
                    },
                    onBack = onBack,
                )
            }

            item {
                ProfileIdentityCard(
                    name = name,
                    isNew = isNew,
                    profileIndex = currentProfile?.profileIndex,
                    usesPrimaryAddons = usesPrimaryAddons,
                    onNameChange = { name = it },
                    onUsesPrimaryAddonsChange = { usesPrimaryAddons = it },
                    selectedAvatar = visibleAvatarItem,
                    customAvatarUrl = customAvatarUrl,
                    accentColor = previewAccent,
                    hasAvatarChoices = avatars.isNotEmpty(),
                )
            }

            item {
                NuvioSurfaceCard {
                    Column(verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10)) {
                        Text(
                            text = stringResource(Res.string.profile_custom_avatar_url),
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.colors.textPrimary,
                        )
                        Text(
                            text = stringResource(Res.string.profile_custom_avatar_url_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.colors.textMuted,
                        )
                        NuvioInputField(
                            value = avatarUrl,
                            onValueChange = { value ->
                                avatarUrl = value
                                if (value.isNotBlank()) {
                                    selectedAvatarId = null
                                }
                            },
                            placeholder = stringResource(Res.string.profile_custom_avatar_url_placeholder),
                        )
                        if (avatarUrlIsInvalid) {
                            Text(
                                text = stringResource(Res.string.profile_avatar_url_invalid),
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.colors.danger,
                            )
                        }
                    }
                }
            }

            if (canChooseBackground) {
                item {
                    NuvioSurfaceCard {
                        Column(verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14)) {
                            Text(
                                text = stringResource(Res.string.profile_choose_background),
                                style = MaterialTheme.typography.titleLarge,
                                color = tokens.colors.textPrimary,
                            )
                            Text(
                                text = stringResource(Res.string.profile_background_member_note),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.colors.textMuted,
                            )
                            ProfileBackgroundPicker(
                                backgrounds = backgroundCatalog,
                                selectedBackgroundId = selectedBackgroundId,
                                selectedBackgroundUrl = selectedBackgroundUrl,
                                customBackgroundUrl = currentProfile?.profileBackgroundUrl,
                                standardBackgroundColor = previewAccent,
                                onSelectionChange = { id, url ->
                                    selectedBackgroundId = id
                                    selectedBackgroundUrl = url
                                },
                            )
                        }
                    }
                }
            }

            item {
                NuvioSurfaceCard {
                    Column(verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14)) {
                        Text(
                            text = stringResource(Res.string.profile_choose_avatar),
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.colors.textPrimary,
                        )
                        Text(
                            text = selectedAvatarItem?.displayName
                                ?: if (avatars.isEmpty()) {
                                    stringResource(Res.string.profile_loading_avatars)
                                } else {
                                    stringResource(Res.string.profile_select_avatar)
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.colors.textMuted,
                        )

                        AvatarPicker(
                            avatars = avatars,
                            selectedAvatarId = selectedAvatarId.takeIf { customAvatarUrl == null },
                            onAvatarSelected = { avatar ->
                                avatarUrl = ""
                                selectedAvatarId = avatar.id
                            },
                        )
                    }
                }
            }

            if (!isNew) {
                item {
                    NuvioSurfaceCard {
                        Column(verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14)) {
                            Text(
                                text = stringResource(Res.string.profile_security),
                                style = MaterialTheme.typography.titleLarge,
                                color = tokens.colors.textPrimary,
                            )
                            Text(
                                text = if (currentProfile?.pinEnabled == true) {
                                    stringResource(Res.string.profile_security_pin_enabled)
                                } else {
                                    stringResource(Res.string.profile_security_pin_disabled)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.colors.textMuted,
                            )
                            if (currentProfile?.pinEnabled == true) {
                                NuvioPrimaryButton(
                                    text = stringResource(Res.string.profile_remove_pin_lock),
                                    onClick = { showPinClear = true },
                                )
                            } else {
                                NuvioPrimaryButton(
                                    text = stringResource(Res.string.profile_set_pin_lock),
                                    onClick = { showPinSetup = true },
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(NuvioTokens.Space.s8))
                val canSave = name.isNotBlank() && !avatarUrlIsInvalid && !isSaving
                val saveShape = tokens.shapes.button
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            val avatarColorHex = visibleAvatarItem?.bgColor ?: fallbackColorHex
                            if (isNew) {
                                ProfileRepository.createProfile(
                                    name = name,
                                    avatarColorHex = avatarColorHex,
                                    avatarId = if (customAvatarUrl == null) selectedAvatarId else null,
                                    avatarUrl = customAvatarUrl,
                                    usesPrimaryAddons = usesPrimaryAddons,
                                )
                            } else {
                                ProfileRepository.updateProfile(
                                    profileIndex = currentProfile!!.profileIndex,
                                    name = name,
                                    avatarColorHex = avatarColorHex,
                                    avatarId = if (customAvatarUrl == null) selectedAvatarId else null,
                                    avatarUrl = customAvatarUrl,
                                    profileBackgroundId = selectedBackgroundId,
                                    profileBackgroundUrl = selectedBackgroundUrl,
                                    usesPrimaryAddons = usesPrimaryAddons,
                                )
                            }
                            isSaving = false
                            onSaved()
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NuvioTokens.Space.s48 + NuvioTokens.Space.s4)
                        .then(if (isDesktop && canSave) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                        .nuvioFocusBorder(saveShape),
                    shape = saveShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = tokens.colors.onAccent,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = tokens.colors.textDisabled,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(saveShape)
                            .then(
                                if (canSave) {
                                    Modifier.background(palette.accentBrush())
                                } else {
                                    Modifier.background(tokens.colors.surfaceElevated)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isSaving) {
                                stringResource(Res.string.profile_saving)
                            } else if (isNew) {
                                stringResource(Res.string.profile_create_profile)
                            } else {
                                stringResource(Res.string.collections_editor_save_changes)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (!isNew && (currentProfile?.profileIndex ?: 0) > 1) {
                item {
                    Spacer(modifier = Modifier.height(NuvioTokens.Space.s8))
                    val deleteInteractionSource = remember { MutableInteractionSource() }
                    val isDeleteHovered by deleteInteractionSource.collectIsHoveredAsState()
                    val deleteShape = tokens.shapes.button
                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NuvioTokens.Space.s48 + NuvioTokens.Space.s4)
                            .then(
                                if (isDesktop) {
                                    Modifier
                                        .hoverable(deleteInteractionSource)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                } else {
                                    Modifier
                                },
                            )
                            .nuvioFocusBorder(deleteShape),
                        interactionSource = deleteInteractionSource,
                        shape = deleteShape,
                        border = BorderStroke(
                            tokens.borders.thin,
                            tokens.colors.danger.copy(alpha = if (isDesktop && isDeleteHovered) 0.65f else 0.35f),
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.colors.danger.copy(alpha = if (isDesktop && isDeleteHovered) 0.22f else 0.14f),
                            contentColor = tokens.colors.danger,
                        ),
                    ) {
                        Text(
                            text = stringResource(Res.string.profile_delete_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    NuvioStatusModal(
        title = stringResource(Res.string.profile_delete_title),
        message = stringResource(
            Res.string.profile_delete_confirm_message,
            currentProfile?.name.orEmpty(),
        ),
        isVisible = showDeleteConfirm,
        destructive = true,
        confirmText = stringResource(Res.string.action_delete),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = {
            showDeleteConfirm = false
            scope.launch {
                currentProfile?.let { ProfileRepository.deleteProfile(it.profileIndex) }
                onBack()
            }
        },
        onDismiss = { showDeleteConfirm = false },
    )

    if (showPinSetup && currentProfile != null) {
        PinSetupDialog(
            profileIndex = currentProfile.profileIndex,
            hasExistingPin = currentProfile.pinEnabled,
            onDone = {
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false },
        )
    }

    if (showPinClear && currentProfile != null) {
        PinEntryDialog(
            profileName = stringResource(Res.string.profile_remove_pin_for, currentProfile.name),
            onVerify = { pin -> ProfileRepository.clearPin(currentProfile.profileIndex, pin) },
            onVerified = {
                showPinClear = false
            },
            onDismiss = {
                showPinClear = false
            },
        )
    }
}

@Composable
private fun ProfileIdentityCard(
    name: String,
    isNew: Boolean,
    profileIndex: Int?,
    usesPrimaryAddons: Boolean,
    onNameChange: (String) -> Unit,
    onUsesPrimaryAddonsChange: (Boolean) -> Unit,
    selectedAvatar: AvatarCatalogItem?,
    customAvatarUrl: String?,
    accentColor: Color,
    hasAvatarChoices: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    NuvioSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s16)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedAvatar != null || customAvatarUrl != null) {
                                accentColor
                            } else {
                                tokens.colors.surfaceElevated
                            },
                        )
                        .border(
                            width = NuvioTokens.Border.medium,
                            color = if (selectedAvatar != null || customAvatarUrl != null) {
                                tokens.colors.accent
                            } else {
                                tokens.colors.accent.copy(alpha = 0.45f)
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (customAvatarUrl != null) {
                        AsyncImage(
                            model = customAvatarUrl,
                            contentDescription = name,
                            modifier = Modifier.size(88.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else if (selectedAvatar != null) {
                        AsyncImage(
                            model = avatarImageUrl(selectedAvatar),
                            contentDescription = selectedAvatar.displayName,
                            modifier = Modifier.size(88.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else if (name.isNotBlank()) {
                        Text(
                            text = name.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
                ) {
                    Text(
                        text = name.ifBlank {
                            if (isNew) stringResource(Res.string.profile_new)
                            else stringResource(Res.string.profile_unnamed)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = listOf(
                            if (isNew) {
                                stringResource(Res.string.profile_new)
                            } else {
                                profileIndex?.let { stringResource(Res.string.profile_label_number, it) }
                                    ?: stringResource(Res.string.profile_unnamed)
                            },
                            if (usesPrimaryAddons) {
                                stringResource(Res.string.profile_primary_addons_on)
                            } else {
                                stringResource(Res.string.profile_primary_addons_off)
                            },
                        ).joinToString("  |  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textSecondary,
                    )
                    Text(
                        text = when {
                            customAvatarUrl != null -> stringResource(Res.string.profile_custom_avatar_selected)
                            selectedAvatar != null -> stringResource(
                                Res.string.profile_avatar_selected,
                                selectedAvatar.displayName,
                            )
                            hasAvatarChoices -> stringResource(Res.string.profile_choose_avatar_below)
                            else -> stringResource(Res.string.profile_avatar_options_pending)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                    )
                }
            }

            NuvioInputField(
                value = name,
                onValueChange = onNameChange,
                placeholder = stringResource(Res.string.profile_name_placeholder),
            )

            HorizontalDivider(
                thickness = tokens.borders.hairline,
                color = tokens.colors.borderSubtle,
            )

            ProfileOptionRow(
                title = stringResource(Res.string.profile_use_primary_addons),
                description = stringResource(Res.string.profile_use_primary_addons_description),
                checked = usesPrimaryAddons,
                onCheckedChange = onUsesPrimaryAddonsChange,
            )
        }
    }
}


@Composable
private fun ProfileOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
        }
        Spacer(modifier = Modifier.width(NuvioTokens.Space.s12))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.colors.onAccent,
                checkedTrackColor = tokens.colors.accent,
                uncheckedThumbColor = tokens.colors.textSecondary,
                uncheckedTrackColor = tokens.colors.surfaceElevated,
                uncheckedBorderColor = tokens.colors.borderDefault,
            ),
        )
    }
}

@Composable
fun PinSetupDialog(
    profileIndex: Int,
    hasExistingPin: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(if (hasExistingPin) "current" else "new") }
    var currentPin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    when (step) {
        "current" -> PinEntryDialog(
            profileName = stringResource(Res.string.profile_enter_current_pin),
            onVerify = { pin -> ProfileRepository.verifyPin(profileIndex, pin) },
            onVerified = { pin ->
                currentPin = pin
                step = "new"
            },
            onDismiss = onDismiss,
        )

        "new" -> PinEntryDialog(
            profileName = stringResource(Res.string.profile_enter_new_pin),
            onVerify = { pin ->
                ProfileRepository.setPin(
                    profileIndex = profileIndex,
                    pin = pin,
                    currentPin = currentPin.ifEmpty { null },
                )
            },
            onVerified = {
                onDone()
            },
            onDismiss = onDismiss,
        )
    }
}
