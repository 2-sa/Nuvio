package com.nuvio.app.features.collection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.core.ui.withDuplicateSafeLazyKeys
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionManagementScreen(
    onBack: () -> Unit,
    onNavigateToEditor: (String?) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val collections by CollectionRepository.collections.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showCopyError by remember { mutableStateOf(false) }

    NuvioScreen {
        stickyHeader {
            Column(modifier = Modifier.fillMaxWidth()) {
                NuvioScreenHeader(
                    title = stringResource(Res.string.collections_header),
                    onBack = onBack,
                ) {
                    HeaderIconPillButton(
                        icon = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(Res.string.collections_copy_json),
                        onClick = {
                            val json = CollectionRepository.exportToJson()
                            showCopyError = runCatching {
                                clipboardManager.setText(AnnotatedString(json))
                            }.isFailure
                        },
                    )
                    Spacer(modifier = Modifier.width(NuvioTokens.Space.s6))
                    HeaderIconPillButton(
                        icon = Icons.Rounded.ContentPaste,
                        contentDescription = stringResource(Res.string.collections_import),
                        onClick = { showImportDialog = true },
                    )
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = tokens.colors.surfaceCard,
                border = BorderStroke(NuvioTokens.Border.thin, tokens.colors.borderSubtle),
            ) {
                Column(
                    modifier = Modifier.padding(tokens.spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14),
                ) {
                    Text(
                        text = stringResource(
                            Res.string.collections_count_summary,
                            collections.size,
                            collections.sumOf { it.folders.size },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.colors.textSecondary,
                    )

                    val ctaShape = RoundedCornerShape(14.dp)
                    val ctaInteraction = remember { MutableInteractionSource() }
                    val isCtaHovered by ctaInteraction.collectIsHoveredAsState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .nuvioFocusBorder(ctaShape)
                            .clip(ctaShape)
                            .background(palette.accentBrush())
                            .then(
                                if (isDesktop && isCtaHovered) {
                                    Modifier.background(Color.White.copy(alpha = NuvioTokens.Opacity.hover))
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (isDesktop) {
                                    Modifier
                                        .hoverable(ctaInteraction)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable(
                                interactionSource = ctaInteraction,
                                indication = null,
                                role = Role.Button,
                                onClick = { onNavigateToEditor(null) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = tokens.colors.onAccent,
                                modifier = Modifier.size(NuvioTokens.Icon.md),
                            )
                            Text(
                                text = stringResource(Res.string.collections_new),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.colors.onAccent,
                            )
                        }
                    }
                }
            }
        }

        if (collections.isNotEmpty()) {
            item { NuvioSectionLabel(text = stringResource(Res.string.collections_your_collections)) }
        }

        if (collections.isNotEmpty()) {
            item {
                CollectionReorderableList(
                    collections = collections,
                    onEdit = { onNavigateToEditor(it) },
                    onDelete = { showDeleteConfirm = it },
                )
            }
        }

        if (collections.isEmpty()) {
            item {
                NuvioSurfaceCard(
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = tokens.colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.collections_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.collections_empty_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.colors.textSecondary,
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        ImportDialog(
            importText = importText,
            importError = importError,
            onTextChange = {
                importText = it
                importError = null
            },
            onConfirm = {
                val result = CollectionRepository.validateJson(importText)
                if (result.valid) {
                    CollectionRepository.importFromJson(importText)
                    showImportDialog = false
                    importText = ""
                    importError = null
                } else {
                    importError = result.error
                }
            },
            onDismiss = {
                showImportDialog = false
                importText = ""
                importError = null
            },
        )
    }

    NuvioStatusModal(
        title = stringResource(Res.string.collections_copy_json),
        message = stringResource(Res.string.collections_copy_json_error),
        isVisible = showCopyError,
        onConfirm = { showCopyError = false },
        onDismiss = { showCopyError = false },
    )

    val deleteId = showDeleteConfirm
    val deleteCollection = deleteId?.let { id -> collections.find { it.id == id } }
    NuvioStatusModal(
        title = stringResource(Res.string.collections_delete_title),
        message = stringResource(Res.string.collections_delete_message, deleteCollection?.title.orEmpty()),
        isVisible = deleteId != null,
        destructive = true,
        confirmText = stringResource(Res.string.action_delete),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = {
            if (deleteId != null) {
                CollectionRepository.removeCollection(deleteId)
            }
            showDeleteConfirm = null
        },
        onDismiss = { showDeleteConfirm = null },
    )
}

@Composable
private fun HeaderIconPillButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .nuvioFocusBorder(CircleShape)
            .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
        shape = CircleShape,
        color = if (isDesktop && isHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
        border = BorderStroke(
            width = NuvioTokens.Border.thin,
            color = if (isDesktop && isHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
        ),
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isDesktop && isHovered) tokens.colors.textPrimary else tokens.colors.textSecondary,
                modifier = Modifier.size(NuvioTokens.Icon.sm + NuvioTokens.Space.s2),
            )
        }
    }
}

@Composable
private fun CollectionReorderableList(
    collections: List<Collection>,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val keyedCollections = remember(collections) {
        collections.withDuplicateSafeLazyKeys { collection -> collection.id }
    }
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        CollectionRepository.moveByIndex(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 720.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
    ) {
        itemsIndexed(keyedCollections, key = { _, collection -> collection.lazyKey }) { _, keyedCollection ->
            val collection = keyedCollection.value
            ReorderableItem(reorderableLazyListState, key = keyedCollection.lazyKey) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp)
                CollectionListItem(
                    collection = collection,
                    isDragging = isDragging,
                    shadowElevation = elevation,
                    onEdit = { onEdit(collection.id) },
                    onDelete = { onDelete(collection.id) },
                    dragHandleScope = this@ReorderableItem,
                )
            }
        }
    }
}

@Composable
private fun CollectionListItem(
    collection: Collection,
    isDragging: Boolean,
    shadowElevation: androidx.compose.ui.unit.Dp,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val tokens = MaterialTheme.nuvio
    val hapticFeedback = LocalHapticFeedback.current
    val cardShape = RoundedCornerShape(16.dp)
    val cardInteraction = remember { MutableInteractionSource() }
    val isHovered by cardInteraction.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = when {
            isDragging -> tokens.colors.accent
            isDesktop && isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "collection_card_border",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDesktop) Modifier.hoverable(cardInteraction) else Modifier),
        shape = cardShape,
        color = if (isDragging) tokens.colors.surfaceElevated else tokens.colors.surfaceCard,
        border = BorderStroke(NuvioTokens.Border.thin, borderColor),
        shadowElevation = shadowElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        ) {
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
                        text = collection.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.collections_folder_count, collection.folders.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.colors.textSecondary,
                        )
                        if (collection.pinToTop) {
                            Surface(
                                shape = tokens.shapes.chip,
                                color = tokens.colors.focusBackground,
                                border = BorderStroke(NuvioTokens.Border.thin, tokens.colors.accent.copy(alpha = 0.55f)),
                            ) {
                                Text(
                                    text = stringResource(Res.string.collections_pinned),
                                    modifier = Modifier.padding(horizontal = NuvioTokens.Space.s8, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.colors.textPrimary,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val dragInteraction = remember { MutableInteractionSource() }
                val isDragHovered by dragInteraction.collectIsHoveredAsState()
                Surface(
                    modifier = with(dragHandleScope) {
                        Modifier
                            .draggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            )
                            .then(
                                if (isDesktop) {
                                    Modifier
                                        .hoverable(dragInteraction)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                } else {
                                    Modifier
                                },
                            )
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDesktop && isDragHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
                    border = BorderStroke(
                        width = NuvioTokens.Border.thin,
                        color = if (isDesktop && isDragHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s6),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(Res.string.action_reorder),
                            modifier = Modifier.size(18.dp),
                            tint = if (isDesktop && isDragHovered) tokens.colors.textPrimary else tokens.colors.textMuted,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                ActionPillButton(
                    icon = Icons.Rounded.Edit,
                    label = stringResource(Res.string.action_edit),
                    tint = tokens.colors.textPrimary,
                    borderColor = tokens.colors.borderDefault,
                    onClick = onEdit,
                )
                ActionPillButton(
                    icon = Icons.Rounded.Delete,
                    label = null,
                    contentDescription = stringResource(Res.string.action_delete),
                    tint = tokens.colors.danger,
                    borderColor = tokens.colors.danger.copy(alpha = 0.40f),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: ImageVector,
    label: String?,
    tint: Color,
    borderColor: Color,
    onClick: () -> Unit,
    contentDescription: String? = null,
) {
    val tokens = MaterialTheme.nuvio
    val pillShape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        onClick = onClick,
        modifier = Modifier
            .nuvioFocusBorder(pillShape)
            .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
        shape = pillShape,
        color = if (isDesktop && isHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
        border = BorderStroke(
            width = NuvioTokens.Border.thin,
            color = if (isDesktop && isHovered) tokens.colors.borderStrong else borderColor,
        ),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (label != null) NuvioTokens.Space.s12 else NuvioTokens.Space.s10,
                vertical = NuvioTokens.Space.s6,
            ),
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(16.dp),
                tint = tint,
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                )
            }
        }
    }
}

@Composable
private fun ImportDialog(
    importText: String,
    importError: String?,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    DialogSurface(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.collections_import_header),
        message = stringResource(Res.string.collections_import_paste_description),
    ) {
        OutlinedTextField(
            value = importText,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            placeholder = {
                Text(
                    text = stringResource(Res.string.collections_import_json_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textMuted,
                )
            },
            isError = importError != null,
            supportingText = importError?.let {
                { Text(it, color = tokens.colors.danger) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            maxLines = 10,
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = tokens.colors.textPrimary,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = tokens.colors.accent,
                unfocusedBorderColor = tokens.colors.borderDefault,
                focusedContainerColor = tokens.colors.surfaceElevated,
                unfocusedContainerColor = tokens.colors.surfaceElevated,
                cursorColor = tokens.colors.accent,
            ),
        )
        DialogButtons {
            DialogButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
            )
            DialogButton(
                text = stringResource(Res.string.action_import),
                onClick = onConfirm,
                enabled = importText.isNotBlank(),
                style = DialogButtonStyle.Primary,
            )
        }
    }
}
