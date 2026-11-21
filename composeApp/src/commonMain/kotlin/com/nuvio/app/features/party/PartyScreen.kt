package com.nuvio.app.features.party

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioIconActionButton
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioPlatformExtraTopPadding
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.player.formatPlaybackTime
import com.nuvio.app.isDesktop
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_remove
import nuvio.composeapp.generated.resources.party_behind
import nuvio.composeapp.generated.resources.party_code_label
import nuvio.composeapp.generated.resources.party_copy_invite
import nuvio.composeapp.generated.resources.party_host
import nuvio.composeapp.generated.resources.party_host_hint
import nuvio.composeapp.generated.resources.party_hosted_by
import nuvio.composeapp.generated.resources.party_invite_copied
import nuvio.composeapp.generated.resources.party_invite_message
import nuvio.composeapp.generated.resources.party_join_action
import nuvio.composeapp.generated.resources.party_join_description
import nuvio.composeapp.generated.resources.party_join_title
import nuvio.composeapp.generated.resources.party_joined_title
import nuvio.composeapp.generated.resources.party_open_seat
import nuvio.composeapp.generated.resources.party_reconnecting
import nuvio.composeapp.generated.resources.party_remove_message
import nuvio.composeapp.generated.resources.party_remove_title
import nuvio.composeapp.generated.resources.party_seats
import nuvio.composeapp.generated.resources.party_title
import nuvio.composeapp.generated.resources.party_watch_with_party
import nuvio.composeapp.generated.resources.party_you
import org.jetbrains.compose.resources.stringResource

private enum class PartyScreenMode {
    Join,
    Joined,
    Party,
}

@Composable
fun PartyScreen(
    initialCode: String,
    onBack: () -> Unit,
    onWatch: (PartyMedia) -> Unit,
) {
    val state by PartyRepository.state.collectAsStateWithLifecycle()
    val companion by PartyCompanion.state.collectAsStateWithLifecycle()
    val linkable = initialCode.isEmpty() && state.party == null
    if (linkable) {
        DisposableEffect(Unit) {
            PartyCompanion.start()
            onDispose { PartyCompanion.leave() }
        }
    }
    AnimatedContent(
        targetState = linkable && companion.active,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
        modifier = Modifier.fillMaxSize(),
    ) { linked ->
        if (linked) {
            PartyCompanionScreen(onBack = onBack)
        } else {
            PartyHome(initialCode = initialCode, onBack = onBack, onWatch = onWatch)
        }
    }
}

@Composable
private fun PartyHome(
    initialCode: String,
    onBack: () -> Unit,
    onWatch: (PartyMedia) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val state by PartyRepository.state.collectAsStateWithLifecycle()
    val party = state.party
    var joined by remember { mutableStateOf<ActiveParty?>(null) }
    var pendingCode by remember {
        mutableStateOf(initialCode.takeIf { isValidPartyCode(it) && normalizePartyCode(it) != party?.code }.orEmpty())
    }
    var joining by remember { mutableStateOf(party == null || pendingCode.isNotEmpty()) }
    val mode = when {
        joined != null -> PartyScreenMode.Joined
        party != null && !joining -> PartyScreenMode.Party
        else -> PartyScreenMode.Join
    }
    LaunchedEffect(joined) {
        val current = joined ?: return@LaunchedEffect
        delay(1600)
        onWatch(current.media)
        joined = null
    }

    AnimatedContent(
        targetState = mode == PartyScreenMode.Party,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
        modifier = Modifier.fillMaxSize(),
    ) { inParty ->
        if (inParty) {
            PartyRoom(onBack = onBack, onWatch = onWatch)
            return@AnimatedContent
        }
        NuvioScreen(modifier = Modifier.fillMaxSize()) {
            stickyHeader {
                Column(modifier = Modifier.fillMaxWidth()) {
                    NuvioScreenHeader(
                        title = stringResource(Res.string.party_title),
                        onBack = onBack,
                    )
                    if (isDesktop) {
                        HorizontalDivider(
                            thickness = NuvioTokens.Border.thin,
                            color = tokens.colors.borderSubtle,
                        )
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (isDesktop) 16.dp else 4.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    AnimatedContent(
                        targetState = mode,
                        transitionSpec = {
                            (fadeIn(tween(260, delayMillis = 60)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith fadeOut(tween(140))
                        },
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth(),
                    ) { target ->
                        when (target) {
                            PartyScreenMode.Join -> JoinContent(
                                initialCode = pendingCode,
                                onJoined = { newParty ->
                                    pendingCode = ""
                                    joining = false
                                    joined = newParty
                                },
                            )
                            PartyScreenMode.Joined -> (joined ?: party)?.let { JoinedContent(it) }
                            PartyScreenMode.Party -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyRoom(
    onBack: () -> Unit,
    onWatch: (PartyMedia) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val state by PartyRepository.state.collectAsStateWithLifecycle()
    val unread by PartyChat.unread.collectAsStateWithLifecycle()
    val party = state.party ?: return
    var showChat by rememberSaveable { mutableStateOf(unread > 0) }
    PartyPage(onBack = onBack) {
        PartyTabs(
            chatSelected = showChat,
            unread = unread,
            onSelect = { showChat = it },
            modifier = Modifier
                .padding(top = 8.dp, bottom = 14.dp)
                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, RoundedCornerShape(12.dp)),
        )
        AnimatedContent(
            targetState = showChat,
            transitionSpec = { fadeIn(tween(200, delayMillis = 90)) togetherWith fadeOut(tween(90)) },
            modifier = Modifier.fillMaxSize(),
        ) { chat ->
            if (chat) {
                val chatShape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = nuvioSafeBottomPadding(12.dp))
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .clip(chatShape)
                        .background(tokens.colors.surfaceElevated)
                        .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, chatShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    PartyChatContent(
                        members = party.members,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = nuvioSafeBottomPadding(tokens.spacing.screenBottom)),
                ) {
                    PartyContent(party, onWatch)
                }
            }
        }
    }
}

@Composable
internal fun PartyPage(
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val horizontalPad = if (isDesktop) NuvioTokens.Space.s24 else tokens.spacing.screenHorizontal
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .padding(top = tokens.spacing.screenTop + statusBarTop + nuvioPlatformExtraTopPadding)
            .padding(horizontal = horizontalPad),
    ) {
        NuvioScreenHeader(
            title = stringResource(Res.string.party_title),
            onBack = onBack,
        )
        if (isDesktop) {
            HorizontalDivider(
                thickness = NuvioTokens.Border.thin,
                color = tokens.colors.borderSubtle,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = if (isDesktop) 740.dp else 520.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            content = content,
        )
    }
}

@Composable
private fun JoinContent(
    initialCode: String,
    onJoined: (ActiveParty) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val state by PartyRepository.state.collectAsStateWithLifecycle()
    val available by PartyRepository.available.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val shake = remember { Animatable(0f) }
    val prefilled = remember { isValidPartyCode(initialCode) }
    var code by remember {
        mutableStateOf(if (prefilled) "" else normalizePartyCode(initialCode).take(PARTY_CODE_LENGTH))
    }
    var error by remember { mutableStateOf<PartyError?>(null) }
    var preview by remember { mutableStateOf<PartyPreview?>(null) }
    var previewing by remember { mutableStateOf(false) }
    var previewJob by remember { mutableStateOf<Job?>(null) }
    val blockedError = when {
        !available -> PartyError.Unavailable
        !PartyRepository.canUse -> PartyError.SignInRequired
        else -> null
    }
    val shownError = blockedError ?: error

    suspend fun reject(result: PartyError) {
        haptic.performHapticFeedback(HapticFeedbackType.Reject)
        error = result
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 420
                -14f at 60
                12f at 120
                -8f at 180
                6f at 240
                -3f at 300
            },
        )
    }

    fun submit() {
        if (!isValidPartyCode(code) || state.busy || blockedError != null) return
        scope.launch {
            error = null
            val result = PartyRepository.join(code)
            val party = PartyRepository.state.value.party
            if (result == null && party != null) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onJoined(party)
            } else {
                reject(result ?: PartyError.Network)
            }
        }
    }

    fun lookUp(target: String) {
        previewJob?.cancel()
        previewing = true
        previewJob = scope.launch {
            val result = PartyRepository.preview(target)
            previewing = false
            val failure = partyErrorOf(result.error)
            when {
                failure == null && result.media != null -> {
                    preview = result
                    if (result.isFull) error = PartyError.Full
                }
                failure != null && failure != PartyError.Network -> reject(failure)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (blockedError != null) return@LaunchedEffect
        if (prefilled) {
            val target = normalizePartyCode(initialCode).take(PARTY_CODE_LENGTH)
            delay(250)
            target.indices.forEach { index ->
                delay(60)
                code = target.take(index + 1)
            }
            lookUp(target)
        } else {
            delay(650)
            runCatching { focusRequester.requestFocus() }
            delay(550)
            runCatching { focusRequester.requestFocus() }
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AnimatedContent(
            targetState = preview,
            contentKey = { it != null },
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 60)) + slideInVertically(tween(300)) { it / 10 }) togetherWith fadeOut(tween(140))
            },
        ) { current ->
            if (current != null) {
                PreviewCard(current)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SeatsHero(
                        size = 72.dp,
                        ringColor = tokens.colors.surfaceCard,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        text = stringResource(Res.string.party_join_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.party_join_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        CodeField(
            code = code,
            onCodeChange = { value ->
                if (state.busy) return@CodeField
                if (value.length > code.length) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                code = value
                error = null
                preview = null
                previewJob?.cancel()
                previewing = false
                if (isValidPartyCode(value)) lookUp(value)
            },
            onDone = ::submit,
            focusRequester = focusRequester,
            enabled = blockedError == null,
            modifier = Modifier
                .padding(top = 4.dp)
                .graphicsLayer { translationX = shake.value.dp.toPx() },
        )
        AnimatedContent(
            targetState = shownError,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 2 }) togetherWith fadeOut(tween(120))
            },
        ) { current ->
            if (current != null) {
                Text(
                    text = stringResource(current.messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.danger,
                    textAlign = TextAlign.Center,
                )
            }
        }
        PartyGradientCtaButton(
            text = stringResource(Res.string.party_join_action),
            enabled = isValidPartyCode(code) && blockedError == null &&
                error !in setOf(PartyError.NotFound, PartyError.Removed, PartyError.RateLimited),
            loading = state.busy || previewing,
            onClick = ::submit,
        )
        ExperimentalNote(textAlign = TextAlign.Center)
        AnimatedVisibility(
            visible = preview == null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(120)),
        ) {
            Text(
                text = stringResource(Res.string.party_host_hint),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PreviewCard(preview: PartyPreview) {
    val tokens = MaterialTheme.nuvio
    val media = preview.media ?: return
    val host = preview.host
    val previewShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(previewShape)
            .background(tokens.colors.surfaceElevated)
            .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, previewShape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MediaHeader(media, centered = true)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            if (host != null) {
                PartyAvatar(member = host, size = 28.dp, presence = PartyPresenceState.Watching, ringColor = tokens.colors.surfaceElevated)
                Text(
                    text = stringResource(Res.string.party_hosted_by, host.displayName),
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(tokens.colors.surfaceCard)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.party_seats, preview.members, preview.maxMembers),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun MediaHeader(media: PartyMedia, centered: Boolean = false) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MediaTitle(media, centered)
        media.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textSecondary,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MediaTitle(media: PartyMedia, centered: Boolean) {
    val tokens = MaterialTheme.nuvio
    var logoFailed by remember(media.logo) { mutableStateOf(false) }
    val logo = media.logo?.takeIf { it.isNotBlank() && !logoFailed }
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = media.title,
                contentScale = ContentScale.Fit,
                alignment = if (centered) Alignment.Center else Alignment.CenterStart,
                onError = { logoFailed = true },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(80.dp),
            )
        } else if (media.title.isNotBlank()) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = tokens.colors.textPrimary,
                textAlign = textAlign,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        media.episodeLabel()?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = tokens.colors.accent,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun JoinedContent(party: ActiveParty) {
    val tokens = MaterialTheme.nuvio
    val host = party.member(party.hostMemberId)?.displayName
    val cardShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarStack(
            members = party.members,
            size = 80.dp,
            ringColor = tokens.colors.surfaceCard,
            animateIn = true,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = stringResource(Res.string.party_joined_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = tokens.colors.textPrimary,
        )
        if (host != null) {
            Text(
                text = stringResource(Res.string.party_hosted_by, host),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textSecondary,
            )
        }
        MediaTitle(
            media = party.media,
            centered = true,
        )
    }
}

@Composable
private fun PartyContent(
    party: ActiveParty,
    onWatch: (PartyMedia) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val invite = stringResource(Res.string.party_invite_message, formatPartyCode(party.code), partyInviteLink(party.code))
    val copiedLabel = stringResource(Res.string.party_invite_copied)
    var copied by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<PartyMember?>(null) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }
    val copyInvite = {
        clipboard.setText(AnnotatedString(invite))
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        copied = true
        NuvioToastController.show(copiedLabel)
    }
    val cardShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Room Media & Code Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(tokens.colors.surfaceCard)
                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MediaHeader(party.media)
            RoomCodePill(
                code = party.code,
                connected = party.connected,
                copied = copied,
                onCopy = copyInvite,
            )
        }

        // Participants Card (avatar chip row with status dots + detailed seat rows)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(tokens.colors.surfaceCard)
                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.party_seats, party.members.size, party.maxMembers),
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.colors.textSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(party.maxMembers) { index ->
                        Box(
                            modifier = Modifier
                                .size(width = 14.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < party.members.size) tokens.colors.accent else tokens.colors.borderDefault,
                                ),
                        )
                    }
                }
            }

            // Participant avatar/chip row with status dots
            ParticipantAvatarChipRow(
                party = party,
                onInvite = if (party.members.size < party.maxMembers) copyInvite else null,
            )

            HorizontalDivider(
                thickness = NuvioTokens.Border.thin,
                color = tokens.colors.borderSubtle,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                party.members.forEach { member ->
                    val isSelf = member.id == party.memberId
                    val behindMs = party.behindMs(member)
                    val status = if (behindMs != null) {
                        stringResource(Res.string.party_behind, formatPlaybackTime(behindMs))
                    } else {
                        stringResource(party.statusRes(member))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PartyAvatar(
                            member = member,
                            size = 40.dp,
                            presence = party.presenceState(member),
                            ringColor = tokens.colors.surfaceCard,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (isSelf) "${member.displayName} (${stringResource(Res.string.party_you)})" else member.displayName,
                                    modifier = Modifier.weight(1f, fill = false),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = tokens.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (member.id == party.hostMemberId) {
                                    Text(
                                        text = stringResource(Res.string.party_host),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.colors.accent,
                                    )
                                }
                            }
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (party.isHost && !isSelf) {
                            NuvioIconActionButton(
                                icon = Icons.Rounded.Close,
                                contentDescription = stringResource(Res.string.action_remove),
                                tint = tokens.colors.textMuted,
                                onClick = { memberToRemove = member },
                            )
                        }
                    }
                }
            }
        }

        // Room Controls Card
        if (party.isHost || party.hostOnly) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(tokens.colors.surfaceCard)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                if (party.isHost) {
                    HostSettings(
                        party = party,
                        onHostOnlyChange = PartyRepository::setHostOnly,
                        onSharingChange = PartyRepository::setSharing,
                    )
                } else if (party.hostOnly) {
                    HostOnlyNote()
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PartyGradientCtaButton(
                text = stringResource(Res.string.party_watch_with_party),
                leadingIcon = Icons.Rounded.PlayArrow,
                enabled = true,
                loading = false,
                onClick = { onWatch(party.media) },
            )
            PartyActions(
                party = party,
                onLeave = PartyRepository::leave,
                onEnd = PartyRepository::end,
                buttonModifier = Modifier.height(50.dp),
            )
        }
    }

    memberToRemove?.let { member ->
        ConfirmDialog(
            title = stringResource(Res.string.party_remove_title, member.displayName),
            message = stringResource(Res.string.party_remove_message),
            confirmLabel = stringResource(Res.string.action_remove),
            onConfirm = {
                memberToRemove = null
                PartyRepository.removeMember(member.id)
            },
            onDismiss = { memberToRemove = null },
        )
    }
}

@Composable
private fun RoomCodePill(
    code: String,
    connected: Boolean,
    copied: Boolean,
    onCopy: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val pillShape = RoundedCornerShape(14.dp)
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "roomCodePillBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(pillShape)
            .background(tokens.colors.surfaceElevated)
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
                onClick = onCopy,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (connected) tokens.colors.success else tokens.colors.warning),
                )
                Text(
                    text = if (connected) {
                        stringResource(Res.string.party_code_label)
                    } else {
                        stringResource(Res.string.party_reconnecting)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (connected) tokens.colors.textMuted else tokens.colors.accent,
                )
            }
            Text(
                text = formatPartyCode(code),
                style = MaterialTheme.typography.headlineSmall.copy(
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = tokens.colors.textPrimary,
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(tokens.colors.surfaceCard)
                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = copied,
                transitionSpec = {
                    (scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), initialScale = 0.5f) + fadeIn()) togetherWith
                        (scaleOut(targetScale = 0.5f) + fadeOut())
                },
            ) { done ->
                Icon(
                    imageVector = if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(Res.string.party_copy_invite),
                    tint = if (done) tokens.colors.accent else tokens.colors.textPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = stringResource(Res.string.party_copy_invite),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (copied) tokens.colors.accent else tokens.colors.textPrimary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ParticipantAvatarChipRow(
    party: ActiveParty,
    onInvite: (() -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val scrollState = rememberScrollState()
    val chipShape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .nuvioDesktopDragScroll(scrollState)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        party.members.forEach { member ->
            val presence = party.presenceState(member)
            val statusColor = when (presence) {
                PartyPresenceState.Watching, PartyPresenceState.Busy -> tokens.colors.success
                PartyPresenceState.Away -> tokens.colors.warning
                PartyPresenceState.Offline -> tokens.colors.textMuted
            }
            Row(
                modifier = Modifier
                    .clip(chipShape)
                    .background(tokens.colors.surfaceElevated)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, chipShape)
                    .padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PartyAvatar(
                    member = member,
                    size = 24.dp,
                    presence = presence,
                    ringColor = tokens.colors.surfaceElevated,
                )
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
            }
        }
        if (onInvite != null) {
            Row(
                modifier = Modifier
                    .clip(chipShape)
                    .background(tokens.colors.surfaceElevated)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, chipShape)
                    .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                    .clickable(onClick = onInvite)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(Res.string.party_open_seat),
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.colors.accent,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PartyGradientCtaButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val ctaShape = RoundedCornerShape(14.dp)
    val interactive = enabled && !loading

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else tokens.opacity.disabled)
            .clip(ctaShape)
            .background(palette.accentBrush())
            .then(
                if (isDesktop && interactive && isHovered) {
                    Modifier.background(Color.White.copy(alpha = NuvioTokens.Opacity.hover))
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(ctaShape)
            .then(
                if (isDesktop && interactive) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = interactive,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = tokens.colors.onAccent,
                strokeWidth = 2.dp,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = tokens.colors.onAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.onAccent,
                )
            }
        }
    }
}
