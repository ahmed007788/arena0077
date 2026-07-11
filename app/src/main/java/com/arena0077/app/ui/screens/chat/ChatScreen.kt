package com.arena0077.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VideoCameraBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arena0077.app.data.models.BattleMode
import com.arena0077.app.data.models.Message
import com.arena0077.app.data.models.MessageRole
import com.arena0077.app.data.models.Modality
import com.arena0077.app.data.models.QuickAction
import com.arena0077.app.ui.theme.ArenaPurple
import com.arena0077.app.ui.theme.ArenaGreen
import com.arena0077.app.ui.theme.ModelAColor
import com.arena0077.app.ui.theme.ModelBColor

/**
 * ChatScreen - the main chat interface.
 *
 * Layout (mirrors arena.ai's actual layout):
 *
 *   ┌──────────────────────────────────────┐
 *   │           [Empty state]              │  ← if no messages
 *   │   "Experience the frontier"          │
 *   │   [Quick action buttons grid]        │
 *   ├──────────────────────────────────────┤
 *   │   Battle Mode  ▼                     │  ← mode selector
 *   ├──────────────────────────────────────┤
 *   │   Message list (LazyColumn)          │
 *   │   ┌──────────────────────────────┐   │
 *   │   │ User: Hello, what is 2+2?    │   │
 *   │   ├──────────────────────────────┤   │
 *   │   │ Model A: 4                   │   │
 *   │   │ Model B: 2 + 2 = 4           │   │
 *   │   └──────────────────────────────┘   │
 *   ├──────────────────────────────────────┤
 *   │ [📎] [Ask anything...        ] [↑]   │  ← input bar
 *   │      [Code] [Search] [Image] [Video] │  ← modality toolbar
 *   └──────────────────────────────────────┘
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val modality by viewModel.modality.collectAsStateWithLifecycle()
    val battleMode by viewModel.battleMode.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Ensure the WebView engine is initialized
    LaunchedEffect(Unit) {
        viewModel.ensureEngineInitialized()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Battle mode selector bar
            BattleModeBar(
                mode = battleMode,
                onModeSelected = { viewModel.setBattleMode(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Messages or empty state
            if (messages.isEmpty()) {
                EmptyChatState(
                    modality = modality,
                    onQuickAction = { viewModel.applyQuickAction(it) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                MessageList(
                    messages = messages,
                    isStreaming = isStreaming,
                    onVote = { viewModel.vote(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Error banner
            if (error != null) {
                ErrorBanner(
                    message = error!!,
                    onDismiss = { viewModel.clearError() }
                )
            }

            // Input bar with modality toolbar
            ChatInputBar(
                modality = modality,
                isStreaming = isStreaming,
                isSending = isSending,
                onModalitySelected = { viewModel.setModality(it) },
                onSend = { viewModel.sendMessage(it) },
                onStop = { viewModel.stop() }
            )
        }
    }
}

@Composable
private fun BattleModeBar(
    mode: BattleMode,
    onModeSelected: (BattleMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BattleMode.values().forEach { m ->
            val isSelected = m == mode
            Surface(
                color = if (isSelected) ArenaPurple.copy(alpha = 0.15f) else Color.Transparent,
                contentColor = if (isSelected) ArenaPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onModeSelected(m) }
            ) {
                Text(
                    text = m.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    modality: Modality,
    onQuickAction: (QuickAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Experience\nthe frontier",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 44.sp
        )

        Spacer(Modifier.height(32.dp))

        // Quick action buttons grid
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAction.values().forEach { action ->
                QuickActionButton(
                    action = action,
                    onClick = { onQuickAction(action) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    action: QuickAction,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .widthIn(min = 150.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    isStreaming: Boolean,
    onVote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(message = message, isStreaming = isStreaming, onVote = onVote)
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isStreaming: Boolean,
    onVote: (String) -> Unit
) {
    val isUser = message.role == MessageRole.USER
    val isModelA = message.role == MessageRole.MODEL_A
    val isModelB = message.role == MessageRole.MODEL_B

    val bubbleColor = when {
        isUser -> ArenaPurple.copy(alpha = 0.15f)
        isModelA -> ModelAColor.copy(alpha = 0.1f)
        isModelB -> ModelBColor.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val labelColor = when {
        isModelA -> ModelAColor
        isModelB -> ModelBColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val senderLabel = when {
        isUser -> "You"
        isModelA -> "Model A"
        isModelB -> "Model B"
        else -> message.modelName ?: "Assistant"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            if (!isUser) {
                Text(
                    text = senderLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = message.content.ifEmpty { "..." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (message.isStreaming && message.content.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(labelColor.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    modality: Modality,
    isStreaming: Boolean,
    isSending: Boolean,
    onModalitySelected: (Modality) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attachment button
                IconButton(
                    onClick = { /* TODO: file picker */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = "Add files",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Text field
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isStreaming) "Ask followup…" else "Ask anything…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Send / Stop button
                if (isStreaming) {
                    IconButton(
                        onClick = {
                            onStop()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ArenaPurple)
                    ) {
                        Icon(
                            Icons.Outlined.Stop,
                            contentDescription = "Stop",
                            tint = Color.White
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSend(inputText)
                                inputText = ""
                                keyboard?.hide()
                            }
                        },
                        enabled = inputText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isSending) ArenaPurple
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) Color.White
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Modality toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModalityChip(
                    icon = Icons.Outlined.Code,
                    label = "Code",
                    isSelected = modality == Modality.WEBDEV,
                    onClick = { onModalitySelected(Modality.WEBDEV) }
                )
                ModalityChip(
                    icon = Icons.Outlined.Search,
                    label = "Search",
                    isSelected = false,  // Search is a sub-modality
                    onClick = { /* Search mode */ }
                )
                ModalityChip(
                    icon = Icons.Outlined.Image,
                    label = "Image",
                    isSelected = modality == Modality.IMAGE,
                    onClick = { onModalitySelected(Modality.IMAGE) }
                )
                ModalityChip(
                    icon = Icons.Outlined.VideoCameraBack,
                    label = "Video",
                    isSelected = modality == Modality.VIDEO,
                    onClick = { onModalitySelected(Modality.VIDEO) }
                )
            }
        }
    }
}

@Composable
private fun ModalityChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) ArenaPurple.copy(alpha = 0.15f) else Color.Transparent,
        contentColor = if (isSelected) ArenaPurple else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        contentColor = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onDismiss() }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(12.dp)
        )
    }
}
