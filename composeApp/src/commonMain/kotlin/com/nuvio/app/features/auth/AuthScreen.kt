package com.nuvio.app.features.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.DeviceLinkAuthRepository
import com.nuvio.app.core.auth.DeviceLinkAuthState
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.settings.AppBrandWordmark
import com.nuvio.app.isDesktop
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_auth_already_have_account
import nuvio.composeapp.generated.resources.compose_auth_continue_without_account
import nuvio.composeapp.generated.resources.compose_auth_create_account
import nuvio.composeapp.generated.resources.compose_auth_dont_have_account
import nuvio.composeapp.generated.resources.compose_auth_email
import nuvio.composeapp.generated.resources.compose_auth_or_separator
import nuvio.composeapp.generated.resources.compose_auth_password
import nuvio.composeapp.generated.resources.compose_auth_sign_in
import nuvio.composeapp.generated.resources.compose_auth_sign_in_subtitle
import nuvio.composeapp.generated.resources.compose_auth_sign_up
import nuvio.composeapp.generated.resources.compose_auth_sign_up_subtitle
import nuvio.composeapp.generated.resources.compose_auth_store_locally
import nuvio.composeapp.generated.resources.compose_auth_tagline
import nuvio.composeapp.generated.resources.compose_auth_welcome_back
import org.jetbrains.compose.resources.stringResource

internal val AuthTextPrimary = Color(0xFFF8FAFC)
internal val AuthTextSecondary = Color(0xFF94A3B8)
internal val AuthFieldBackground = Color(0xFF111622)
internal val AuthFieldBorder = Color(0xFF24324A)

private data class AuthFormMetrics(
    val fieldHeight: Dp,
    val fieldHorizontalPadding: Dp,
    val iconSize: Dp,
    val passwordIconSize: Dp,
    val primaryTop: Dp,
    val primaryHeight: Dp,
    val toggleTop: Dp,
    val dividerTop: Dp,
    val secondaryTop: Dp,
    val secondaryHeight: Dp,
)

private val MobileAuthFormMetrics = AuthFormMetrics(
    fieldHeight = 54.dp,
    fieldHorizontalPadding = 16.dp,
    iconSize = 19.dp,
    passwordIconSize = 20.dp,
    primaryTop = 20.dp,
    primaryHeight = 52.dp,
    toggleTop = 16.dp,
    dividerTop = 24.dp,
    secondaryTop = 22.dp,
    secondaryHeight = 52.dp,
)

private val DesktopAuthFormMetrics = AuthFormMetrics(
    fieldHeight = 52.dp,
    fieldHorizontalPadding = 16.dp,
    iconSize = 19.dp,
    passwordIconSize = 20.dp,
    primaryTop = 20.dp,
    primaryHeight = 50.dp,
    toggleTop = 16.dp,
    dividerTop = 24.dp,
    secondaryTop = 20.dp,
    secondaryHeight = 48.dp,
)

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
) {
    val authError by AuthRepository.error.collectAsStateWithLifecycle()
    val deviceLinkAuthState by DeviceLinkAuthRepository.state.collectAsStateWithLifecycle()
    val serverConnectionState by ServerConnectionController.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var isSignUp by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var emailFieldBounds by remember { mutableStateOf<Rect?>(null) }
    var passwordFieldBounds by remember { mutableStateOf<Rect?>(null) }
    var showServerSheet by rememberSaveable { mutableStateOf(false) }
    var showOfficialServerDialog by rememberSaveable { mutableStateOf(false) }

    fun submitAuth() {
        if (email.isBlank() || password.length < 6 || isLoading) return
        DeviceLinkAuthRepository.cancel()
        isLoading = true
        focusManager.clearFocus(force = true)
        scope.launch {
            if (isSignUp) AuthRepository.signUpWithEmail(email, password)
            else AuthRepository.signInWithEmail(email, password)
            isLoading = false
        }
    }

    fun toggleAuthMode() {
        DeviceLinkAuthRepository.cancel()
        isSignUp = !isSignUp
        AuthRepository.clearError()
    }

    fun startDeviceLink() {
        if (isLoading) return
        focusManager.clearFocus(force = true)
        AuthRepository.clearError()
        DeviceLinkAuthRepository.start()
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(serverConnectionState.activeServer.backendUrl) {
        DeviceLinkAuthRepository.cancel()
        if (!serverConnectionState.activeServer.isCustom) {
            showOfficialServerDialog = false
        }
    }

    DisposableEffect(Unit) {
        onDispose(DeviceLinkAuthRepository::cancel)
    }

    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .pointerInput(emailFieldBounds, passwordFieldBounds) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val tappedTextField = listOfNotNull(emailFieldBounds, passwordFieldBounds)
                        .any { bounds -> bounds.contains(down.position) }
                    if (!tappedTextField) {
                        focusManager.clearFocus(force = true)
                    }
                }
            },
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val screenWidth = maxWidth
            val largeScreen = screenWidth >= 720.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .authObsidianBackdrop(
                        background = tokens.colors.background,
                        accent = tokens.colors.accent,
                    ),
            ) {
                if (largeScreen) {
                    AuthLargeLayout(
                        modifier = Modifier.fillMaxSize(),
                        isSignUp = isSignUp,
                        email = email,
                        password = password,
                        passwordVisible = passwordVisible,
                        isLoading = isLoading,
                        authError = authError,
                        deviceLinkAuthState = deviceLinkAuthState,
                        deviceLinkEnabled = serverConnectionState.activeServer.capabilities.tvLogin,
                        statusBarTop = statusBarTop,
                        onEmailChange = {
                            email = it
                            AuthRepository.clearError()
                        },
                        onPasswordChange = {
                            password = it
                            AuthRepository.clearError()
                        },
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        onSubmit = ::submitAuth,
                        onToggleAuthMode = ::toggleAuthMode,
                        onContinueWithoutAccount = {
                            focusManager.clearFocus(force = true)
                            DeviceLinkAuthRepository.cancel()
                            AuthRepository.signInAnonymously()
                        },
                        onStartDeviceLink = ::startDeviceLink,
                        onCancelDeviceLink = DeviceLinkAuthRepository::cancel,
                        onEmailBoundsChange = { emailFieldBounds = it },
                        onPasswordBoundsChange = { passwordFieldBounds = it },
                    )
                } else {
                    AuthMobileLayout(
                        isSignUp = isSignUp,
                        email = email,
                        password = password,
                        passwordVisible = passwordVisible,
                        isLoading = isLoading,
                        authError = authError,
                        deviceLinkAuthState = deviceLinkAuthState,
                        deviceLinkEnabled = serverConnectionState.activeServer.capabilities.tvLogin,
                        statusBarTop = statusBarTop,
                        onEmailChange = {
                            email = it
                            AuthRepository.clearError()
                        },
                        onPasswordChange = {
                            password = it
                            AuthRepository.clearError()
                        },
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        onSubmit = ::submitAuth,
                        onToggleAuthMode = ::toggleAuthMode,
                        onContinueWithoutAccount = {
                            focusManager.clearFocus(force = true)
                            DeviceLinkAuthRepository.cancel()
                            AuthRepository.signInAnonymously()
                        },
                        onStartDeviceLink = ::startDeviceLink,
                        onCancelDeviceLink = DeviceLinkAuthRepository::cancel,
                        onEmailBoundsChange = { emailFieldBounds = it },
                        onPasswordBoundsChange = { passwordFieldBounds = it },
                    )
                }
            }
        }

        if (AppFeaturePolicy.customServerConnectionsEnabled) {
            ServerConnectionMenu(
                activeServer = serverConnectionState.activeServer,
                onUseOfficial = {
                    ServerConnectionController.resetDiscovery()
                    showOfficialServerDialog = true
                },
                onConnectCustom = {
                    ServerConnectionController.resetDiscovery()
                    showServerSheet = true
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = statusBarTop + 4.dp),
            )
        }
    }

    if (
        AppFeaturePolicy.customServerConnectionsEnabled &&
        showServerSheet &&
        serverConnectionState.discoveredServer == null
    ) {
        ServerConnectionSheet(
            state = serverConnectionState,
            onDiscover = ServerConnectionController::discover,
            onDismiss = {
                showServerSheet = false
                ServerConnectionController.resetDiscovery()
            },
        )
    }

    serverConnectionState.discoveredServer?.let { server ->
        if (AppFeaturePolicy.customServerConnectionsEnabled) {
            ServerTrustDialog(
                server = server,
                isSwitching = serverConnectionState.isSwitching,
                switchFailure = serverConnectionState.switchFailure,
                onConfirm = ServerConnectionController::connectDiscovered,
                onDismiss = {
                    showServerSheet = false
                    ServerConnectionController.resetDiscovery()
                },
            )
        }
    }

    if (AppFeaturePolicy.customServerConnectionsEnabled && showOfficialServerDialog) {
        OfficialServerDialog(
            isSwitching = serverConnectionState.isSwitching,
            switchFailure = serverConnectionState.switchFailure,
            onConfirm = ServerConnectionController::useOfficial,
            onDismiss = {
                showOfficialServerDialog = false
                ServerConnectionController.resetDiscovery()
            },
        )
    }
}

@Composable
private fun AuthMobileLayout(
    isSignUp: Boolean,
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    authError: String?,
    deviceLinkAuthState: DeviceLinkAuthState,
    deviceLinkEnabled: Boolean,
    statusBarTop: Dp,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onToggleAuthMode: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onStartDeviceLink: () -> Unit,
    onCancelDeviceLink: () -> Unit,
    onEmailBoundsChange: (Rect) -> Unit,
    onPasswordBoundsChange: (Rect) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = NuvioTokens.Space.s24,
                top = statusBarTop + NuvioTokens.Space.s48,
                end = NuvioTokens.Space.s24,
                bottom = NuvioTokens.Space.s36,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthBrandLockup(logoHeight = 38.dp)

            Spacer(modifier = Modifier.height(NuvioTokens.Space.s40))

            AuthHeading(
                isSignUp = isSignUp,
                headingStyle = MaterialTheme.typography.headlineLarge.copy(
                    color = tokens.colors.textPrimary,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                subtitleStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = tokens.colors.textSecondary,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )

            Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))

            AuthForm(
                isSignUp = isSignUp,
                email = email,
                password = password,
                passwordVisible = passwordVisible,
                isLoading = isLoading,
                authError = authError,
                deviceLinkAuthState = deviceLinkAuthState,
                deviceLinkEnabled = deviceLinkEnabled,
                metrics = MobileAuthFormMetrics,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                onSubmit = onSubmit,
                onToggleAuthMode = onToggleAuthMode,
                onContinueWithoutAccount = onContinueWithoutAccount,
                onStartDeviceLink = onStartDeviceLink,
                onCancelDeviceLink = onCancelDeviceLink,
                onEmailBoundsChange = onEmailBoundsChange,
                onPasswordBoundsChange = onPasswordBoundsChange,
            )
        }
    }
}

@Composable
private fun AuthLargeLayout(
    modifier: Modifier = Modifier,
    isSignUp: Boolean,
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    authError: String?,
    deviceLinkAuthState: DeviceLinkAuthState,
    deviceLinkEnabled: Boolean,
    statusBarTop: Dp,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onToggleAuthMode: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onStartDeviceLink: () -> Unit,
    onCancelDeviceLink: () -> Unit,
    onEmailBoundsChange: (Rect) -> Unit,
    onPasswordBoundsChange: (Rect) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(NuvioTokens.Space.s20)
    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = NuvioTokens.Space.s32,
                top = statusBarTop + NuvioTokens.Space.s32,
                end = NuvioTokens.Space.s32,
                bottom = NuvioTokens.Space.s32,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            shape = cardShape,
            color = tokens.colors.surfaceCard,
            border = BorderStroke(tokens.borders.thin, tokens.colors.borderDefault),
            shadowElevation = tokens.elevation.modal,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NuvioTokens.Space.s32, vertical = NuvioTokens.Space.s32),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuthBrandLockup(logoHeight = 36.dp)

                Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))

                HorizontalDivider(
                    thickness = tokens.borders.hairline,
                    color = tokens.colors.borderSubtle,
                )

                Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))

                AuthHeading(
                    isSignUp = isSignUp,
                    headingStyle = MaterialTheme.typography.headlineLarge.copy(
                        color = tokens.colors.textPrimary,
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    subtitleStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = tokens.colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )

                Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))

                AuthForm(
                    isSignUp = isSignUp,
                    email = email,
                    password = password,
                    passwordVisible = passwordVisible,
                    isLoading = isLoading,
                    authError = authError,
                    deviceLinkAuthState = deviceLinkAuthState,
                    deviceLinkEnabled = deviceLinkEnabled,
                    metrics = DesktopAuthFormMetrics,
                    scale = 1f,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                    onSubmit = onSubmit,
                    onToggleAuthMode = onToggleAuthMode,
                    onContinueWithoutAccount = onContinueWithoutAccount,
                    onStartDeviceLink = onStartDeviceLink,
                    onCancelDeviceLink = onCancelDeviceLink,
                    onEmailBoundsChange = onEmailBoundsChange,
                    onPasswordBoundsChange = onPasswordBoundsChange,
                )
            }
        }
    }
}

@Composable
private fun AuthBrandLockup(
    logoHeight: Dp,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBrandWordmark(
            contentDescription = null,
            modifier = Modifier.height(logoHeight),
        )
        Spacer(modifier = Modifier.height(NuvioTokens.Space.s10))
        Text(
            text = stringResource(Res.string.compose_auth_tagline),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = tokens.colors.textMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthHeading(
    isSignUp: Boolean,
    headingStyle: TextStyle,
    subtitleStyle: TextStyle,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
    ) {
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authHeading",
        ) { signUp ->
            Text(
                text = if (signUp) stringResource(Res.string.compose_auth_create_account)
                else stringResource(Res.string.compose_auth_welcome_back),
                style = headingStyle,
                color = tokens.colors.textPrimary,
            )
        }
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authSubtitle",
        ) { signUp ->
            Text(
                text = if (signUp) stringResource(Res.string.compose_auth_sign_up_subtitle)
                else stringResource(Res.string.compose_auth_sign_in_subtitle),
                style = subtitleStyle,
                color = tokens.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun AuthForm(
    isSignUp: Boolean,
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    authError: String?,
    deviceLinkAuthState: DeviceLinkAuthState,
    deviceLinkEnabled: Boolean,
    metrics: AuthFormMetrics,
    scale: Float = 1f,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onToggleAuthMode: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onStartDeviceLink: () -> Unit,
    onCancelDeviceLink: () -> Unit,
    onEmailBoundsChange: (Rect) -> Unit,
    onPasswordBoundsChange: (Rect) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = stringResource(Res.string.compose_auth_email),
            icon = Icons.Rounded.Email,
            metrics = metrics,
            scale = scale,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                onEmailBoundsChange(coordinates.boundsInRoot())
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = stringResource(Res.string.compose_auth_password),
            icon = Icons.Rounded.Lock,
            metrics = metrics,
            scale = scale,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibilityToggle = onPasswordVisibilityToggle,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                onPasswordBoundsChange(coordinates.boundsInRoot())
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() },
            ),
        )

        authError?.let { errorText ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = tokens.colors.danger,
                    fontSize = (13f * scale).sp,
                    lineHeight = (18f * scale).sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(metrics.primaryTop))

        AuthPrimaryButton(
            text = if (isSignUp) stringResource(Res.string.compose_auth_create_account)
            else stringResource(Res.string.compose_auth_sign_in),
            isLoading = isLoading,
            enabled = !isLoading,
            height = metrics.primaryHeight,
            scale = scale,
            onClick = onSubmit,
        )

        Spacer(modifier = Modifier.height(metrics.toggleTop))

        AuthModeToggle(
            isSignUp = isSignUp,
            scale = scale,
            onToggleAuthMode = onToggleAuthMode,
        )

        Spacer(modifier = Modifier.height(metrics.dividerTop))

        AuthDivider(scale = scale)

        Spacer(modifier = Modifier.height(metrics.secondaryTop))

        if (!isSignUp && deviceLinkEnabled) {
            DeviceLinkAuthSection(
                state = deviceLinkAuthState,
                enabled = !isLoading,
                height = metrics.secondaryHeight,
                scale = scale,
                onStart = onStartDeviceLink,
                onCancel = onCancelDeviceLink,
            )

            Spacer(modifier = Modifier.height(14.dp * scale))
        }

        AuthSecondaryButton(
            text = stringResource(Res.string.compose_auth_continue_without_account),
            enabled = !isLoading,
            height = metrics.secondaryHeight,
            scale = scale,
            onClick = onContinueWithoutAccount,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(Res.string.compose_auth_store_locally),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = tokens.colors.textMuted,
                fontSize = (13f * scale).sp,
                lineHeight = (18f * scale).sp,
                fontWeight = FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    metrics: AuthFormMetrics,
    scale: Float,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(NuvioTokens.Radius.lg + NuvioTokens.Space.s2)
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> tokens.colors.accent
            isDesktop && isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderDefault
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "authFieldBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "authFieldBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.fieldHeight)
            .clip(shape)
            .background(containerColor, shape)
            .border(tokens.borders.thin, borderColor, shape)
            .then(if (isDesktop) Modifier.hoverable(interactionSource) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { focusRequester.requestFocus() }
            .padding(horizontal = metrics.fieldHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(metrics.iconSize),
            tint = if (isFocused) tokens.colors.accent else tokens.colors.textMuted,
        )
        Spacer(modifier = Modifier.width(NuvioTokens.Space.s12))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = tokens.colors.textPrimary,
                fontSize = (15f * scale).sp,
                lineHeight = (20f * scale).sp,
                fontWeight = FontWeight.Normal,
            ),
            cursorBrush = SolidColor(tokens.colors.accent),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = tokens.colors.textMuted,
                                fontSize = (15f * scale).sp,
                                lineHeight = (20f * scale).sp,
                                fontWeight = FontWeight.Normal,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (isPassword) {
            Spacer(modifier = Modifier.width(NuvioTokens.Space.s10))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                    .clickable(onClick = onPasswordVisibilityToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff
                    else Icons.Rounded.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(metrics.passwordIconSize),
                    tint = tokens.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
internal fun AuthPrimaryButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    height: Dp,
    scale: Float,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val shape = RoundedCornerShape(NuvioTokens.Radius.xl)
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .then(if (isDesktop && enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .nuvioFocusBorder(shape),
        enabled = enabled,
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = tokens.colors.onAccent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = if (isLoading) tokens.colors.onAccent else tokens.colors.textDisabled,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .then(
                    if (enabled || isLoading) {
                        Modifier.background(palette.accentBrush())
                    } else {
                        Modifier.background(tokens.colors.surfaceElevated)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                NuvioLoadingIndicator(
                    modifier = Modifier.size(20.dp * scale),
                    color = tokens.colors.onAccent,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = (15f * scale).sp,
                        lineHeight = (20f * scale).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AuthModeToggle(
    isSignUp: Boolean,
    scale: Float,
    onToggleAuthMode: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authTogglePrompt",
        ) { signUp ->
            Text(
                text = if (signUp) {
                    stringResource(Res.string.compose_auth_already_have_account).trimEnd()
                } else {
                    stringResource(Res.string.compose_auth_dont_have_account).trimEnd()
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = tokens.colors.textSecondary,
                    fontSize = (14f * scale).sp,
                    lineHeight = (20f * scale).sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        }
        Spacer(modifier = Modifier.width(NuvioTokens.Space.s6))
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authToggleAction",
        ) { signUp ->
            Text(
                text = if (signUp) stringResource(Res.string.compose_auth_sign_in)
                else stringResource(Res.string.compose_auth_sign_up),
                modifier = Modifier
                    .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                    .clickable(onClick = onToggleAuthMode),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = tokens.colors.accent,
                    fontSize = (14f * scale).sp,
                    lineHeight = (20f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun AuthDivider(scale: Float) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(tokens.borders.hairline)
                .background(tokens.colors.borderSubtle),
        )
        Text(
            text = stringResource(Res.string.compose_auth_or_separator).trim(),
            modifier = Modifier.padding(horizontal = NuvioTokens.Space.s16),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = tokens.colors.textMuted,
                fontSize = (13f * scale).sp,
                lineHeight = (18f * scale).sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(tokens.borders.hairline)
                .background(tokens.colors.borderSubtle),
        )
    }
}

@Composable
internal fun AuthSecondaryButton(
    text: String,
    enabled: Boolean,
    height: Dp,
    scale: Float,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(NuvioTokens.Radius.xl)
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (isDesktop && enabled) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(shape),
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        border = BorderStroke(
            tokens.borders.thin,
            if (isDesktop && enabled && isHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
        ),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDesktop && enabled && isHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
            contentColor = tokens.colors.textPrimary,
            disabledContainerColor = tokens.colors.surfaceElevated.copy(alpha = tokens.opacity.disabled),
            disabledContentColor = tokens.colors.textDisabled,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (15f * scale).sp,
                lineHeight = (20f * scale).sp,
                fontWeight = FontWeight.Medium,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

private fun Modifier.authObsidianBackdrop(
    background: Color,
    accent: Color,
): Modifier = drawWithCache {
    val maxDim = maxOf(size.width, size.height).coerceAtLeast(1f)
    val topCenter = Offset(size.width / 2f, size.height * 0.14f)
    val radialBrush = Brush.radialGradient(
        colorStops = arrayOf(
            0f to accent.copy(alpha = 0.08f),
            0.45f to accent.copy(alpha = 0.035f),
            1f to Color.Transparent,
        ),
        center = topCenter,
        radius = maxDim * 0.55f,
    )
    val verticalGlow = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to accent.copy(alpha = 0.08f),
            0.35f to accent.copy(alpha = 0.02f),
            0.7f to Color.Transparent,
        ),
    )
    onDrawBehind {
        drawRect(color = background)
        drawRect(brush = verticalGlow)
        drawRect(brush = radialBrush)
    }
}

private fun LayoutCoordinates.boundsInRoot(): Rect {
    val position = positionInRoot()
    return Rect(
        left = position.x,
        top = position.y,
        right = position.x + size.width,
        bottom = position.y + size.height,
    )
}
