package com.nuvio.app.features.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.LocalVerticalBarPadding
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioDesktopAnimation
import com.nuvio.app.core.ui.NuvioToastHost
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.features.settings.MemberBrandWordmark
import com.nuvio.app.isDesktop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSelectionScreen(
    onProfileSelected: (NuvioProfile) -> Unit,
    onEditProfile: (NuvioProfile) -> Unit,
    onAddProfile: () -> Unit,
    onBack: (() -> Unit)? = null,
    interactionEnabled: Boolean = true,
    activeProfileIndex: Int? = null,
    contentVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pinDialogProfile by remember { mutableStateOf<NuvioProfile?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var hoveredProfileIndex by remember { mutableStateOf<Int?>(null) }

    val titleAlpha = remember { Animatable(if (isDesktop) 1f else 0f) }
    val titleOffset = remember { Animatable(if (isDesktop) 0f else 20f) }
    val manageAlpha = remember { Animatable(if (isDesktop) 1f else 0f) }
    val onProfileClick: (NuvioProfile) -> Unit = { profile ->
        if (interactionEnabled) {
            routeProfileSelection(
                profile = profile,
                isEditMode = isEditMode,
                activeProfileIndex = activeProfileIndex,
                onEditProfile = onEditProfile,
                onActiveProfileSelected = { scope.launch { showAlreadyActiveProfileToast(it) } },
                onPinRequired = { pinDialogProfile = it },
                onProfileSelected = onProfileSelected,
            )
        }
    }

    LaunchedEffect(Unit) {
        AvatarRepository.refreshAvatars()
    }

    LaunchedEffect(Unit) {
        if (isDesktop) return@LaunchedEffect
        launch { titleAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        launch { titleOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        delay(300)
        manageAlpha.animateTo(1f, tween(500))
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val backgroundProfile = profileState.activeProfile ?: profileState.profiles.firstOrNull()
    val hoveredProfileColor = remember(profileState.profiles, hoveredProfileIndex) {
        if (!isDesktop) {
            null
        } else {
            profileState.profiles
                .firstOrNull { it.profileIndex == hoveredProfileIndex }
                ?.avatarColorHex
                ?.let(::parseHexColor)
        }
    }

    LaunchedEffect(profileState.profiles) {
        if (hoveredProfileIndex != null && profileState.profiles.none { it.profileIndex == hoveredProfileIndex }) {
            hoveredProfileIndex = null
        }
    }

    fun updateHoveredProfile(profile: NuvioProfile, isHovered: Boolean) {
        hoveredProfileIndex = if (isHovered) {
            profile.profileIndex
        } else {
            hoveredProfileIndex.takeUnless { it == profile.profileIndex }
        }
    }

    val tokens = MaterialTheme.nuvio
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize(),
    ) {
        val isTabletLayout = maxWidth >= 768.dp
        ProfileBackgroundBackdrop(
            profile = backgroundProfile,
            colorOverride = hoveredProfileColor,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = NuvioTokens.Space.s24, vertical = NuvioTokens.Space.s24),
                verticalArrangement = if (isTabletLayout) Arrangement.Center else Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(if (isTabletLayout) 0.dp else 44.dp))

                MemberBrandWordmark(
                    height = if (isTabletLayout) 42.dp else 34.dp,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAlpha.value
                        translationY = titleOffset.value
                    },
                )

                Spacer(modifier = Modifier.height(if (isTabletLayout) NuvioTokens.Space.s24 else NuvioTokens.Space.s18))

                Text(
                    text = stringResource(Res.string.profile_who_is_watching),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = if (isTabletLayout) 32.sp else 28.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAlpha.value
                        translationY = titleOffset.value
                    },
                )

                Spacer(modifier = Modifier.height(if (isTabletLayout) NuvioTokens.Space.s32 else NuvioTokens.Space.s40))

                val profiles = profileState.profiles
                val items = profiles.size + if (isEditMode && profiles.size < MAX_PROFILES) 1 else 0

                if (isTabletLayout) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s20, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s20),
                    ) {
                        for (currentIndex in 0 until items) {
                            if (currentIndex < profiles.size) {
                                val profile = profiles[currentIndex]
                                ProfileAvatarCard(
                                    profile = profile,
                                    isEditMode = isEditMode,
                                    animDelay = currentIndex * 80,
                                    enabled = interactionEnabled,
                                    onHoverChange = { isHovered -> updateHoveredProfile(profile, isHovered) },
                                    onClick = {
                                        onProfileClick(profile)
                                    },
                                )
                            } else {
                                AddProfileCard(
                                    animDelay = currentIndex * 80,
                                    enabled = interactionEnabled,
                                    onClick = onAddProfile,
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s16),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        var index = 0
                        while (index < items) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s16, Alignment.CenterHorizontally),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                for (col in 0..1) {
                                    if (index < items) {
                                        val currentIndex = index
                                        if (currentIndex < profiles.size) {
                                            val profile = profiles[currentIndex]
                                            ProfileAvatarCard(
                                                profile = profile,
                                                isEditMode = isEditMode,
                                                animDelay = currentIndex * 80,
                                                enabled = interactionEnabled,
                                                onHoverChange = { isHovered -> updateHoveredProfile(profile, isHovered) },
                                                onClick = {
                                                    onProfileClick(profile)
                                                },
                                            )
                                        } else {
                                            AddProfileCard(
                                                animDelay = currentIndex * 80,
                                                enabled = interactionEnabled,
                                                onClick = onAddProfile,
                                            )
                                        }
                                        index++
                                    } else {
                                        if (profiles.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(156.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isTabletLayout) NuvioTokens.Space.s32 else NuvioTokens.Space.s40))

                val manageInteractionSource = remember { MutableInteractionSource() }
                val isManageHovered by manageInteractionSource.collectIsHoveredAsState()
                val managePillShape = RoundedCornerShape(NuvioTokens.Radius.full)
                val manageContainerColor by animateColorAsState(
                    targetValue = when {
                        isEditMode -> tokens.colors.focusBackground
                        isDesktop && isManageHovered -> tokens.colors.surfaceCard
                        else -> tokens.colors.surfaceElevated.copy(alpha = 0.85f)
                    },
                    animationSpec = tween(durationMillis = tokens.motion.fastMillis),
                    label = "manageProfilesBg",
                )
                val manageBorderColor by animateColorAsState(
                    targetValue = when {
                        isEditMode -> tokens.colors.accent
                        isDesktop && isManageHovered -> tokens.colors.borderStrong
                        else -> tokens.colors.borderDefault
                    },
                    animationSpec = tween(durationMillis = tokens.motion.fastMillis),
                    label = "manageProfilesBorder",
                )

                Surface(
                    modifier = Modifier
                        .graphicsLayer { alpha = manageAlpha.value }
                        .clip(managePillShape)
                        .then(
                            if (isDesktop && interactionEnabled) {
                                Modifier
                                    .hoverable(manageInteractionSource)
                                    .pointerHoverIcon(PointerIcon.Hand)
                            } else {
                                Modifier
                            },
                        )
                        .nuvioFocusBorder(managePillShape)
                        .clickable(
                            enabled = interactionEnabled,
                            interactionSource = manageInteractionSource,
                            indication = null,
                        ) { isEditMode = !isEditMode },
                    shape = managePillShape,
                    color = manageContainerColor,
                    border = BorderStroke(tokens.borders.thin, manageBorderColor),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = NuvioTokens.Space.s24,
                            vertical = NuvioTokens.Space.s10,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = if (isEditMode) tokens.colors.accent else tokens.colors.textSecondary,
                            modifier = Modifier.size(tokens.icons.sm),
                        )
                        Text(
                            text = if (isEditMode) {
                                stringResource(Res.string.action_done)
                            } else {
                                stringResource(Res.string.profile_manage_profiles)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isEditMode) tokens.colors.textPrimary else tokens.colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isTabletLayout) 0.dp else NuvioTokens.Space.s24))
            }
        }

        if (onBack != null && interactionEnabled && contentVisible) {
            NuvioBackButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(LocalVerticalBarPadding.current)
                    .padding(start = 16.dp, top = statusBarTop + 8.dp),
            )
        }

        NuvioToastHost(modifier = Modifier.align(Alignment.TopCenter))
    }

    pinDialogProfile?.let { profile ->
        PinEntryDialog(
            profileName = profile.name,
            onVerify = { pin -> ProfileRepository.verifyPin(profile.profileIndex, pin) },
            onVerified = {
                pinDialogProfile = null
                if (interactionEnabled && profile.profileIndex != activeProfileIndex) {
                    onProfileSelected(profile)
                }
            },
            onDismiss = { pinDialogProfile = null },
        )
    }
}

@Composable
private fun ProfileAvatarCard(
    profile: NuvioProfile,
    isEditMode: Boolean,
    animDelay: Int,
    enabled: Boolean,
    onHoverChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val avatarColor = remember(profile.avatarColorHex) {
        parseHexColor(profile.avatarColorHex)
    }
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    val avatarItem = remember(profile.avatarId, avatars) {
        profile.avatarId?.let { id -> avatars.find { it.id == id } }
    }
    val avatarImageUrl = remember(profile.avatarUrl, avatarItem) {
        profileAvatarImageUrl(profile, avatarItem)
    }

    val animAlpha = remember { Animatable(if (isDesktop) 1f else 0f) }
    val animScale = remember { Animatable(if (isDesktop) 1f else 0.85f) }
    val animOffset = remember { Animatable(if (isDesktop) 0f else 30f) }

    LaunchedEffect(Unit) {
        if (isDesktop) return@LaunchedEffect
        delay(animDelay.toLong() + 150)
        launch { animAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        launch { animScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { animOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val currentOnHoverChange = rememberUpdatedState(onHoverChange)
    val hoverScale by animateFloatAsState(
        targetValue = if (isDesktop && isHovered) 1.04f else 1f,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "profileCardHoverScale",
    )
    val pressScale = if (isPressed && !isDesktop) 0.95f else 1f
    val borderWidth by animateDpAsState(
        targetValue = if (isDesktop && isHovered) NuvioTokens.Border.medium else tokens.borders.thin,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "profileCardBorderWidth",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) tokens.colors.accent else tokens.colors.borderSubtle,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "profileCardBorderColor",
    )
    val tileColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated.copy(alpha = 0.9f),
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "profileCardBg",
    )

    LaunchedEffect(isHovered, profile.profileIndex) {
        if (isDesktop) {
            currentOnHoverChange.value(isHovered)
        }
    }

    DisposableEffect(profile.profileIndex) {
        onDispose {
            if (isDesktop) {
                currentOnHoverChange.value(false)
            }
        }
    }

    val profileShape = RoundedCornerShape(NuvioTokens.Space.s20)
    Surface(
        modifier = Modifier
            .width(156.dp)
            .graphicsLayer {
                alpha = animAlpha.value
                scaleX = animScale.value * pressScale * hoverScale
                scaleY = animScale.value * pressScale * hoverScale
                translationY = animOffset.value
            }
            .clip(profileShape)
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(profileShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = profileShape,
        color = tileColor,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = NuvioTokens.Space.s14, vertical = NuvioTokens.Space.s18),
        ) {
            Box(
                modifier = Modifier.size(106.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarImageUrl != null) {
                    val bgColor = avatarItem?.bgColor?.let { parseHexColor(it) } ?: avatarColor
                    Box(
                        modifier = Modifier
                            .size(106.dp)
                            .clip(CircleShape)
                            .background(bgColor.copy(alpha = 0.22f)),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            if (avatarItem != null) {
                                avatarItem.bgColor?.let { parseHexColor(it) } ?: avatarColor
                            } else {
                                avatarColor.copy(alpha = 0.16f)
                            },
                        )
                        .then(
                            if (avatarImageUrl == null) Modifier.border(NuvioTokens.Border.medium, avatarColor.copy(alpha = 0.45f), CircleShape)
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarImageUrl != null) {
                        AsyncImage(
                            model = avatarImageUrl,
                            contentDescription = avatarItem?.displayName ?: profile.name,
                            modifier = Modifier.size(96.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            desktopAnimation = NuvioDesktopAnimation.Always,
                        )
                    } else if (profile.name.isNotBlank()) {
                        Text(
                            text = profile.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                            color = avatarColor,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = avatarColor,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tokens.colors.accent)
                            .border(NuvioTokens.Border.medium, tokens.colors.surfaceCard, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = tokens.colors.onAccent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (profile.pinEnabled && !isEditMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(tokens.colors.surfaceElevated)
                            .border(NuvioTokens.Border.medium, tokens.colors.surfaceCard, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = tokens.colors.textSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(NuvioTokens.Space.s12))

            Text(
                text = profile.name.ifBlank {
                    stringResource(Res.string.profile_label_number, profile.profileIndex)
                },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = if (isDesktop && isHovered) tokens.colors.textPrimary else tokens.colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AddProfileCard(
    animDelay: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val animAlpha = remember { Animatable(if (isDesktop) 1f else 0f) }
    val animScale = remember { Animatable(if (isDesktop) 1f else 0.85f) }
    val animOffset = remember { Animatable(if (isDesktop) 0f else 30f) }

    LaunchedEffect(Unit) {
        if (isDesktop) return@LaunchedEffect
        delay(animDelay.toLong() + 150)
        launch { animAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        launch { animScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { animOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverScale by animateFloatAsState(
        targetValue = if (isDesktop && isHovered) 1.04f else 1f,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "addProfileHoverScale",
    )
    val pressScale = if (isPressed && !isDesktop) 0.95f else 1f
    val borderWidth by animateDpAsState(
        targetValue = if (isDesktop && isHovered) NuvioTokens.Border.medium else tokens.borders.thin,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "addProfileBorderWidth",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) tokens.colors.accent else tokens.colors.borderSubtle,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "addProfileBorderColor",
    )

    val addProfileShape = RoundedCornerShape(NuvioTokens.Space.s20)
    Surface(
        modifier = Modifier
            .width(156.dp)
            .graphicsLayer {
                alpha = animAlpha.value
                scaleX = animScale.value * pressScale * hoverScale
                scaleY = animScale.value * pressScale * hoverScale
                translationY = animOffset.value
            }
            .clip(addProfileShape)
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(addProfileShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = addProfileShape,
        color = if (isDesktop && isHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated.copy(alpha = 0.72f),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = NuvioTokens.Space.s14, vertical = NuvioTokens.Space.s18),
        ) {
            Box(
                modifier = Modifier.size(106.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(tokens.colors.surfaceCard)
                        .border(
                            tokens.borders.thin,
                            if (isDesktop && isHovered) tokens.colors.accent.copy(alpha = 0.5f) else tokens.colors.borderDefault,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = if (isDesktop && isHovered) tokens.colors.accent else tokens.colors.textMuted,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(NuvioTokens.Space.s12))

            Text(
                text = stringResource(Res.string.compose_profile_add_profile),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = if (isDesktop && isHovered) tokens.colors.textPrimary else tokens.colors.textMuted,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
