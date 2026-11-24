package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioTypeScale
import com.nuvio.app.isDesktop
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_save
import nuvio.composeapp.generated.resources.settings_api_key_clear
import nuvio.composeapp.generated.resources.settings_api_key_paste
import nuvio.composeapp.generated.resources.settings_api_key_reset_default
import nuvio.composeapp.generated.resources.settings_api_key_test
import nuvio.composeapp.generated.resources.settings_api_key_test_default
import nuvio.composeapp.generated.resources.settings_api_key_test_empty
import nuvio.composeapp.generated.resources.settings_api_key_test_invalid
import nuvio.composeapp.generated.resources.settings_api_key_test_latency_ms
import nuvio.composeapp.generated.resources.settings_api_key_test_network_error
import nuvio.composeapp.generated.resources.settings_api_key_test_rate_limited
import nuvio.composeapp.generated.resources.settings_api_key_test_success
import nuvio.composeapp.generated.resources.settings_api_key_test_success_detail
import nuvio.composeapp.generated.resources.settings_api_key_test_tmdb_v4_token
import nuvio.composeapp.generated.resources.settings_api_key_testing
import nuvio.composeapp.generated.resources.settings_hide_secret
import nuvio.composeapp.generated.resources.settings_show_secret
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsApiKeyCard(
    title: String,
    description: String,
    placeholder: String,
    savedValue: String,
    statusLabel: String,
    statusTone: SettingsStatusTone,
    isTablet: Boolean,
    supportsDefaultFallback: Boolean = false,
    supportingNote: String? = null,
    onSave: (String) -> Unit,
    onTestKey: suspend (String) -> ApiKeyTestResult,
) {
    val tokens = MaterialTheme.nuvio
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable(savedValue) { mutableStateOf(savedValue) }
    var visible by rememberSaveable { mutableStateOf(false) }
    var testResult by remember(savedValue) { mutableStateOf<ApiKeyTestResult>(ApiKeyTestResult.Idle) }
    val normalizedDraft = draft.trim()
    val hasChanges = normalizedDraft != savedValue
    val isTesting = testResult is ApiKeyTestResult.Testing
    val canTest = !isTesting && (normalizedDraft.isNotBlank() || supportsDefaultFallback)

    val horizontalPadding = when {
        isDesktop -> 0.dp
        isTablet -> NuvioTokens.Space.s20
        else -> NuvioTokens.Space.s16
    }
    val verticalPadding = when {
        isDesktop -> NuvioTokens.Space.s14
        isTablet -> NuvioTokens.Space.s18
        else -> NuvioTokens.Space.s14
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textSecondary,
                )
            }
            SettingsStatusPill(
                label = statusLabel,
                tone = statusTone,
            )
        }

        if (!supportingNote.isNullOrBlank()) {
            Text(
                text = supportingNote,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.accent,
            )
        }

        OutlinedTextField(
            value = draft,
            onValueChange = {
                draft = it
                if (testResult !is ApiKeyTestResult.Idle) {
                    testResult = ApiKeyTestResult.Idle
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                textDirection = TextDirection.Ltr,
                color = tokens.colors.textPrimary,
            ),
            visualTransformation = if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    IconButton(
                        onClick = { visible = !visible },
                        modifier = Modifier
                            .size(34.dp)
                            .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                    ) {
                        Icon(
                            imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = stringResource(
                                if (visible) Res.string.settings_hide_secret else Res.string.settings_show_secret,
                            ),
                            tint = tokens.colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                            if (pasted.isNotEmpty()) {
                                draft = pasted
                                testResult = ApiKeyTestResult.Idle
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = stringResource(Res.string.settings_api_key_paste),
                            tint = tokens.colors.textSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    if (draft.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                draft = ""
                                testResult = ApiKeyTestResult.Idle
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(Res.string.settings_api_key_clear),
                                tint = tokens.colors.textMuted,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = tokens.colors.accent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.13f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.035f),
                disabledContainerColor = Color.White.copy(alpha = 0.03f),
                cursorColor = tokens.colors.accent,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        if (!canTest) return@OutlinedButton
                        testResult = ApiKeyTestResult.Testing
                        scope.launch {
                            testResult = onTestKey(normalizedDraft)
                        }
                    },
                    enabled = canTest,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = tokens.borders.thin,
                        color = if (canTest) tokens.colors.accent.copy(alpha = 0.45f) else tokens.colors.borderSubtle,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = tokens.colors.accent.copy(alpha = 0.10f),
                        contentColor = tokens.colors.textPrimary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = tokens.colors.textMuted,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .then(if (isDesktop && canTest) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                ) {
                    if (isTesting) {
                        NuvioLoadingIndicator(
                            color = tokens.colors.accent,
                            modifier = Modifier.size(15.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = if (canTest) tokens.colors.accent else tokens.colors.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isTesting -> stringResource(Res.string.settings_api_key_testing)
                            normalizedDraft.isBlank() && supportsDefaultFallback ->
                                stringResource(Res.string.settings_api_key_test_default)
                            else -> stringResource(Res.string.settings_api_key_test)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Button(
                    onClick = {
                        draft = normalizedDraft
                        onSave(normalizedDraft)
                    },
                    enabled = hasChanges,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.colors.accent,
                        contentColor = tokens.colors.onAccent,
                        disabledContainerColor = tokens.colors.surfaceElevated,
                        disabledContentColor = tokens.colors.textMuted,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .then(if (isDesktop && hasChanges) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                ) {
                    Text(
                        text = stringResource(Res.string.action_save),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (savedValue.isNotBlank() && supportsDefaultFallback) {
                TextButton(
                    onClick = {
                        draft = ""
                        testResult = ApiKeyTestResult.Idle
                        onSave("")
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = tokens.colors.textSecondary),
                    modifier = if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_api_key_reset_default),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        AnimatedVisibility(visible = testResult is ApiKeyTestResult.Success || testResult is ApiKeyTestResult.Failure) {
            ApiKeyTestResultBanner(result = testResult)
        }
    }
}

@Composable
internal fun ApiKeyTestResultBanner(
    result: ApiKeyTestResult,
    successOverrideText: String? = null,
    failureOverrideText: String? = null,
) {
    val tokens = MaterialTheme.nuvio
    val isSuccess = result is ApiKeyTestResult.Success
    val accentColor = if (isSuccess) Color(0xFF34D399) else Color(0xFFF87171)
    val message = when (result) {
        is ApiKeyTestResult.Success -> when {
            !successOverrideText.isNullOrBlank() -> successOverrideText
            !result.detail.isNullOrBlank() -> stringResource(
                Res.string.settings_api_key_test_success_detail,
                result.detail,
            )
            else -> stringResource(Res.string.settings_api_key_test_success)
        }
        is ApiKeyTestResult.Failure -> when {
            !failureOverrideText.isNullOrBlank() -> failureOverrideText
            else -> when (result.error) {
                ApiKeyTestError.EmptyKey -> stringResource(Res.string.settings_api_key_test_empty)
                ApiKeyTestError.TmdbV4TokenDetected -> stringResource(Res.string.settings_api_key_test_tmdb_v4_token)
                ApiKeyTestError.InvalidKey -> stringResource(Res.string.settings_api_key_test_invalid)
                ApiKeyTestError.RateLimited -> stringResource(Res.string.settings_api_key_test_rate_limited)
                ApiKeyTestError.InvalidUrlPattern -> stringResource(Res.string.settings_api_key_test_invalid)
                ApiKeyTestError.NetworkError -> stringResource(Res.string.settings_api_key_test_network_error)
            }
        }
        else -> return
    }
    val latencyMs = when (result) {
        is ApiKeyTestResult.Success -> result.latencyMs
        is ApiKeyTestResult.Failure -> result.latencyMs
        else -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accentColor.copy(alpha = 0.11f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(tokens.borders.thin, accentColor.copy(alpha = 0.34f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (latencyMs != null && latencyMs >= 0L) {
                Text(
                    text = stringResource(Res.string.settings_api_key_test_latency_ms, latencyMs),
                    style = MaterialTheme.nuvioTypeScale.labelXs.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    ),
                    color = accentColor.copy(alpha = 0.9f),
                )
            }
        }
    }
}
