package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nuvio.app.isDesktop
import com.nuvio.app.isWindows

private val SheetShape = RoundedCornerShape(NuvioTokens.Space.s28)
private val DesktopDialogShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuvioModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.nuvio.colors.surfaceSheet,
    contentColor: Color = MaterialTheme.nuvio.colors.textPrimary,
    showDragHandle: Boolean = true,
    fullHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isDesktop) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            CompositionLocalProvider(
                LocalBottomInsetsConsumed provides true,
                LocalContentColor provides contentColor,
            ) {
                Surface(
                    modifier = Modifier
                        .padding(NuvioTokens.Space.s24)
                        .widthIn(min = 320.dp, max = if (fullHeight) 620.dp else 520.dp)
                        .heightIn(max = 680.dp)
                        .then(modifier)
                        .border(1.dp, SurfaceEdge, DesktopDialogShape),
                    shape = DesktopDialogShape,
                    color = containerColor,
                    contentColor = contentColor,
                    shadowElevation = 16.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        content = content,
                    )
                }
            }
        }
        return
    }
    if (usesNativeNuvioBottomSheet) {
        NuvioNativeModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            showDragHandle = showDragHandle,
            fullHeight = fullHeight,
            content = content,
        )
    } else {
        val tokens = MaterialTheme.nuvio
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            shape = RectangleShape,
            containerColor = Color.Transparent,
            contentColor = contentColor,
            scrimColor = tokens.colors.overlayScrim,
            dragHandle = null,
        ) {
            CompositionLocalProvider(LocalBottomInsetsConsumed provides true) {
                Column(
                    modifier = modifier
                        .padding(NuvioTokens.Space.s10)
                        .clip(SheetShape)
                        .background(containerColor)
                        .border(tokens.borders.thin, SurfaceEdge, SheetShape),
                ) {
                    if (showDragHandle) {
                        NuvioBottomSheetDragHandle(Modifier.align(Alignment.CenterHorizontally))
                    }
                    if (isWindows) {
                        // Inside the sheet's draggable, so content scrolls before
                        // the sheet can claim its drag. Keep the handle outside
                        // this scope so dragging it still moves the sheet.
                        Column(modifier = Modifier.platformTouchScroll(), content = content)
                    } else {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun NuvioBottomSheetDivider(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    HorizontalDivider(
        modifier = modifier.padding(horizontal = tokens.spacing.screenHorizontal),
        thickness = tokens.borders.hairline,
        color = tokens.colors.borderDefault,
    )
}

@Composable
fun NuvioBottomSheetActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowBackground = when {
        selected -> tokens.colors.accent.copy(alpha = tokens.opacity.hover)
        isDesktop && isHovered -> tokens.colors.textPrimary.copy(alpha = 0.06f)
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NuvioTokens.Space.s8)
            .heightIn(min = if (isDesktop) 46.dp else NuvioTokens.Space.s56)
            .clip(tokens.shapes.compactCard)
            .background(rowBackground)
            .then(if (isDesktop) Modifier.hoverable(interactionSource).pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .clickable(onClick = onClick)
            .padding(NuvioTokens.Space.s8),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(NuvioTokens.Space.s36)
                    .clip(tokens.shapes.compactCard)
                    .background(tokens.colors.accent.copy(alpha = tokens.opacity.selected)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                    modifier = Modifier.size(NuvioTokens.Icon.md),
                )
            }
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else null,
            color = tokens.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailingContent?.invoke(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun dismissNuvioBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    if (isDesktop) {
        onDismiss()
        return
    }
    if (usesNativeNuvioBottomSheet) {
        dismissNativeNuvioBottomSheet()
    } else if (sheetState.isVisible) {
        sheetState.hide()
    }
    onDismiss()
}

@Composable
private fun NuvioBottomSheetDragHandle(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier
            .padding(top = NuvioTokens.Space.s10, bottom = NuvioTokens.Space.s4)
            .size(width = NuvioTokens.Space.s36, height = NuvioTokens.Space.s4)
            .clip(tokens.shapes.chip)
            .background(tokens.colors.borderStrong),
    )
}
