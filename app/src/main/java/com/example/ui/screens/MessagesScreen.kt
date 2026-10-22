package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CoParentMessage
import com.example.data.remote.ToneSuggestionResult
import com.example.ui.components.EmptyRecordsIllustration
import com.example.ui.components.SectionHeader
import com.example.ui.theme.TealSecondary
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MessagesScreen(
    currentUserId: String,
    coParentName: String?,
    defaultChildName: String,
    messages: List<CoParentMessage>,
    toneSuggestion: ToneSuggestionResult?,
    isToneLoading: Boolean,
    onSendMessage: (body: String, attachmentName: String, attachmentUri: String, toneAssisted: Boolean) -> Unit,
    onRequestCalmRewrite: (String, String) -> Unit,
    onDismissToneSuggestion: () -> Unit,
    onEditOrRetractMessage: (CoParentMessage, String, Boolean) -> Unit,
    onMarkMessageRead: (CoParentMessage) -> Unit,
    onExportTranscript: () -> Unit
) {
    var draftText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var attachmentName by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf("") }
    var usedToneAssist by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<CoParentMessage?>(null) }
    var editDraft by remember { mutableStateOf("") }

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentUri = uri.toString()
            attachmentName = "shared_attachment_${System.currentTimeMillis() % 10000}.jpg"
        }
    }

    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) messages
        else messages.filter {
            it.body.contains(searchQuery, ignoreCase = true) ||
                it.originalBody.contains(searchQuery, ignoreCase = true) ||
                it.senderName.contains(searchQuery, ignoreCase = true)
        }
    }

    // Automatically mark incoming co-parent messages as read
    LaunchedEffect(messages, currentUserId) {
        messages.forEach { msg ->
            if (msg.senderUid != currentUserId && currentUserId !in msg.readByUids) {
                onMarkMessageRead(msg)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        SectionHeader(
            title = "Secure Co-Parent Messaging",
            subtitle = "Connected with: ${coParentName?.takeIf { it.isNotBlank() } ?: "Invite Co-Parent to Sync"} • Tamper-Evident Audit Log",
            actionText = "Export Log",
            onAction = onExportTranscript
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Searchable Message History Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Messages") },
            placeholder = { Text("Search message history, dates, or original edits...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("messages_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Message List
        Box(modifier = Modifier.weight(1f)) {
            if (filteredMessages.isEmpty()) {
                EmptyRecordsIllustration(
                    title = if (searchQuery.isBlank()) "No Co-Parent Messages Yet" else "No Matching Messages",
                    subtitle = "All messages include immutable timestamps, read receipts, and full edit/retraction logs for mediation or legal review."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMessages, key = { it.id }) { msg ->
                        val isMe = msg.senderUid == currentUserId
                        MessageBubbleItem(
                            message = msg,
                            isMe = isMe,
                            onStartEdit = {
                                editingMessage = msg
                                editDraft = msg.body
                            },
                            onRetract = {
                                onEditOrRetractMessage(msg, msg.body, true)
                            }
                        )
                    }
                }
            }
        }

        // Optional AI Calm Tone Assistant Card (Never auto-sends)
        if (toneSuggestion != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                ),
                border = BorderStroke(1.dp, TealSecondary)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = TealSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Calm, Child-Focused Suggestion (Review Before Sending)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = toneSuggestion.suggestedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = toneSuggestion.toneNotes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                draftText = toneSuggestion.suggestedText
                                usedToneAssist = true
                                onDismissToneSuggestion()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                            modifier = Modifier.testTag("apply_tone_suggestion_button")
                        ) {
                            Text("Apply to Composer")
                        }
                        TextButton(onClick = onDismissToneSuggestion) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        // Edit Modal Bar if editing an existing message
        if (editingMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Editing Message (Original text will remain visible in audit history)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editDraft,
                            onValueChange = { editDraft = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val target = editingMessage
                                if (target != null && editDraft.isNotBlank()) {
                                    onEditOrRetractMessage(target, editDraft, false)
                                    editingMessage = null
                                }
                            }
                        ) {
                            Text("Save Edit")
                        }
                        TextButton(onClick = { editingMessage = null }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Composer Row with Optional Calm Tone Assistant Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (attachmentName.isNotBlank()) {
                    Text(
                        text = "Attachment ready: $attachmentName",
                        style = MaterialTheme.typography.labelMedium,
                        color = TealSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    placeholder = { Text("Write a clear, child-focused message...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("message_composer_input"),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                onRequestCalmRewrite(draftText, defaultChildName)
                            },
                            enabled = !isToneLoading,
                            modifier = Modifier.testTag("ai_tone_assistant_button")
                        ) {
                            if (isToneLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Calm Tone Rewrite")
                        }

                        IconButton(
                            onClick = {
                                attachmentPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach File")
                        }
                    }

                    Button(
                        onClick = {
                            if (draftText.isNotBlank()) {
                                onSendMessage(draftText, attachmentName, attachmentUri, usedToneAssist)
                                draftText = ""
                                attachmentName = ""
                                attachmentUri = ""
                                usedToneAssist = false
                            }
                        },
                        enabled = draftText.isNotBlank(),
                        modifier = Modifier.testTag("send_message_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleItem(
    message: CoParentMessage,
    isMe: Boolean,
    onStartEdit: () -> Unit,
    onRetract: () -> Unit
) {
    var showOriginalAudit by remember { mutableStateOf(false) }
    val displayTime = remember(message.createdAt, message.updatedAt) {
        val ts: Timestamp? = message.updatedAt ?: message.createdAt
        if (ts != null) {
            SimpleDateFormat("MMM d, HH:mm", Locale.US).format(ts.toDate())
        } else {
            "Just now"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            border = if (isMe) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMe) Color(0xFF99F6E4) else TealSecondary
                    )
                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (message.attachmentName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Attachment: ${message.attachmentName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) Color(0xFFCCFBF1) else TealSecondary
                    )
                }

                if (message.isEdited || message.isRetracted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (message.isRetracted) "Retracted (Audit preserved)" else "Edited (Audit preserved)",
                            style = MaterialTheme.typography.labelSmall
                        )
                        TextButton(onClick = { showOriginalAudit = !showOriginalAudit }) {
                            Text(
                                text = if (showOriginalAudit) "Hide Original" else "View Original",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMe) Color(0xFF99F6E4) else TealSecondary
                            )
                        }
                    }
                    if (showOriginalAudit) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Original Sent Text: \"${message.originalBody}\"\nAudit Log: ${message.auditNote}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (message.readByUids.size >= 2) Color(0xFF5EEAD4)
                            else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (message.readByUids.size >= 2) "Read by Co-Parent" else "Delivered • Timestamped",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (isMe && !message.isRetracted) {
                        Row {
                            IconButton(
                                onClick = onStartEdit,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Message", modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = onRetract,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = "Retract Message", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
