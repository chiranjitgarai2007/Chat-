package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Message
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(viewModel: ChatViewModel) {
    val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val typingStatuses by viewModel.typingStatuses.collectAsStateWithLifecycle()

    var inputMessageText by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to bottom when messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val currentChat = activeChat ?: return

    val filteredMessages = if (chatSearchQuery.isEmpty()) {
        messages
    } else {
        messages.filter { it.text.contains(chatSearchQuery, ignoreCase = true) }
    }

    val typingText = typingStatuses[currentChat.id]

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(
                                name = currentChat.name,
                                colorIndex = if (currentChat.isGroup) 5 else (currentChat.id.hashCode() % 5).let { if (it < 0) -it else it },
                                status = if (currentChat.isGroup) "Offline" else "Online",
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentChat.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentChat.isGroup) "Group conversation" else "Encrypted connection",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchActive = !searchActive }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search messages")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Sub-Bar for chat search
                AnimatedVisibility(visible = searchActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatSearchQuery,
                            onValueChange = { chatSearchQuery = it },
                            placeholder = { Text("Search text in this chat...") },
                            leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { chatSearchQuery = ""; searchActive = false }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close search bar")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                // Interactive attachments tray
                AnimatedVisibility(visible = showAttachmentMenu) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            AttachmentShortcutItem(
                                icon = Icons.Filled.Image,
                                label = "Photo",
                                tint = Color(0xFF24A148),
                                onClick = {
                                    viewModel.sendMessage(
                                        text = "Sent a secure photo wireframe",
                                        fileUri = "mock://image/asset_canvas.png",
                                        fileType = "IMAGE",
                                        fileName = "chat_screen.png"
                                    )
                                    showAttachmentMenu = false
                                }
                            )
                            AttachmentShortcutItem(
                                icon = Icons.Filled.Article,
                                label = "Document",
                                tint = Color(0xFF0F62FE),
                                onClick = {
                                    viewModel.sendMessage(
                                        text = "Posted financial summary breakdown pdf.",
                                        fileUri = "mock://docs/audit_be_v3.pdf",
                                        fileType = "DOCUMENT",
                                        fileName = "audit_be_v3.pdf"
                                    )
                                    showAttachmentMenu = false
                                }
                            )
                            AttachmentShortcutItem(
                                                    icon = Icons.Filled.Mic,
                                                    label = "Audio Tone",
                                                    tint = Color(0xFFDA1E28),
                                                    onClick = {
                                                        viewModel.sendMessage(
                                                            text = "Voice note dispatch (0:32 seconds)",
                                                            fileUri = "mock://audio/voice_clip_rec.mp3",
                                                            fileType = "AUDIO",
                                                            fileName = "voice_memo.mp3"
                                                        )
                                                        showAttachmentMenu = false
                                                    }
                                                )
                                                AttachmentShortcutItem(
                                                    icon = Icons.Filled.Videocam,
                                                    label = "Video",
                                                    tint = Color(0xFF8A3FFC),
                                                    onClick = {
                                                        viewModel.sendMessage(
                                                            text = "Screencast tutorial record walkthrough",
                                                            fileUri = "mock://video/demo_walkthrough.mp4",
                                                            fileType = "VIDEO",
                                                            fileName = "tutorial.mp4"
                                                        )
                                                        showAttachmentMenu = false
                                                    }
                                                )
                        }
                    }
                }

                // Main Text Typing inputs Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentMenu = !showAttachmentMenu },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (showAttachmentMenu) Icons.Filled.Close else Icons.Filled.AttachFile,
                            contentDescription = "Attach metadata",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedTextField(
                        value = inputMessageText,
                        onValueChange = { inputMessageText = it },
                        placeholder = { Text("Type a Message...") },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field"),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputMessageText.trim().isNotEmpty()) {
                                viewModel.sendMessage(inputMessageText)
                                inputMessageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔒 Messages are encrypted in sandbox transit.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(filteredMessages) { msg ->
                    val isOwn = msg.senderId == viewModel.currentUser.value?.id
                    MessageBubble(msg, isOwn)
                }

                if (typingText != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = typingText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentShortcutItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MessageBubble(msg: Message, isOwn: Boolean) {
    val bubbleColor = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isOwn) {
                Text(
                    text = msg.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = bubbleColor, contentColor = contentColor),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOwn) 16.dp else 4.dp,
                    bottomEnd = if (isOwn) 4.dp else 16.dp
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Check file types attachment visual
                    if (msg.fileType != "NONE") {
                        MessageAttachmentPreview(msg)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = msg.text,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // Unread seen details indicator ticks
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = formatTimestamp(msg.timestamp),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                if (isOwn) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (msg.isSeen) Icons.Filled.DoneAll else Icons.Filled.Done,
                        contentDescription = if (msg.isSeen) "Seen" else "Delivered",
                        tint = if (msg.isSeen) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageAttachmentPreview(msg: Message) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fileIcon = when (msg.fileType) {
                "IMAGE" -> Icons.Filled.Image
                "DOCUMENT" -> Icons.Filled.Article
                "AUDIO" -> Icons.Filled.Audiotrack
                else -> Icons.Filled.Videocam
            }

            val iconColor = when (msg.fileType) {
                "IMAGE" -> Color(0xFF24A148)
                "DOCUMENT" -> Color(0xFF0F62FE)
                "AUDIO" -> Color(0xFFDA1E28)
                else -> Color(0xFF8A3FFC)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = fileIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = msg.fileName ?: "Attachment.file",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = "${msg.fileType} • Click to View",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
