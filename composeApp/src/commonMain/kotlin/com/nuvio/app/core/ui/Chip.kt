package com.nuvio.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import com.nuvio.app.isDesktop

@Composable
fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val chipShape = RoundedCornerShape(NuvioTokens.Radius.md)
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.then(
            if (isDesktop && enabled) {
                Modifier.pointerHoverIcon(PointerIcon.Hand)
            } else {
                Modifier
            },
        ),
        enabled = enabled,
        shape = chipShape,
        color = when {
            selected -> tokens.colors.focusBackground
            isDesktop && enabled && isHovered -> tokens.colors.surfaceCard
            else -> tokens.colors.surfaceElevated
        },
        contentColor = when {
            !enabled -> tokens.colors.textDisabled
            selected || (isDesktop && isHovered) -> tokens.colors.textPrimary
            else -> tokens.colors.textMuted
        },
        border = BorderStroke(
            width = tokens.borders.thin,
            color = when {
                selected -> tokens.colors.accent
                !enabled -> tokens.colors.borderSubtle
                isDesktop && isHovered -> tokens.colors.borderStrong
                else -> tokens.colors.borderDefault
            },
        ),
        interactionSource = interactionSource,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = NuvioTokens.Space.s12,
                vertical = tokens.components.chipVerticalPadding,
            ),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
