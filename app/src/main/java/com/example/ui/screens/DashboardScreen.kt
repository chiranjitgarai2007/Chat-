package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Chat
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardTab
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: ChatViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Row(modifier = Modifier.fillMaxSize()) {
        // Horizontal Rail for Wide Screens
        if (isTablet) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Icon(
                        imageVector = Icons.Filled.QuestionAnswer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 24.dp).size(32.dp)
                    )
                },
                modifier = Modifier.fillMaxHeight()
            ) {
                NavigationRailItem(
                    selected = currentTab == DashboardTab.CHATS,
                    onClick = { viewModel.switchTab(DashboardTab.CHATS) },
                    icon = { Icon(Icons.Filled.Chat, contentDescription = "Chats") },
                    label = { Text("Chats") }
                )
                NavigationRailItem(
                    selected = currentTab == DashboardTab.CONTACTS,
                    onClick = { viewModel.switchTab(DashboardTab.CONTACTS) },
                    icon = { Icon(Icons.Filled.People, contentDescription = "Contacts") },
                    label = { Text("Contacts") }
                )
                NavigationRailItem(
                    selected = currentTab == DashboardTab.SEARCH_MESSAGES,
                    onClick = { viewModel.switchTab(DashboardTab.SEARCH_MESSAGES) },
                    icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
                NavigationRailItem(
                    selected = currentTab == DashboardTab.SETTINGS,
                    onClick = { viewModel.switchTab(DashboardTab.SETTINGS) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }

        // Shared Screen Area
        Scaffold(
            bottomBar = {
                if (!isTablet) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == DashboardTab.CHATS,
                            onClick = { viewModel.switchTab(DashboardTab.CHATS) },
                            icon = { Icon(Icons.Filled.Chat, contentDescription = "Chats") },
                            label = { Text("Chats", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentTab == DashboardTab.CONTACTS,
                            onClick = { viewModel.switchTab(DashboardTab.CONTACTS) },
                            icon = { Icon(Icons.Filled.People, contentDescription = "Contacts") },
                            label = { Text("Contacts", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentTab == DashboardTab.SEARCH_MESSAGES,
                            onClick = { viewModel.switchTab(DashboardTab.SEARCH_MESSAGES) },
                            icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                            label = { Text("Search", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentTab == DashboardTab.SETTINGS,
                            onClick = { viewModel.switchTab(DashboardTab.SETTINGS) },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 11.sp) }
                        )
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    DashboardTab.CHATS -> ChatListTab(viewModel)
                    DashboardTab.CONTACTS -> ContactsTab(viewModel)
                    DashboardTab.SEARCH_MESSAGES -> MessageSearchTab(viewModel)
                    DashboardTab.SETTINGS -> SettingsTab(viewModel)
                }

                // In-App Notification HUD alerts
                AppNotificationBanner(viewModel)
            }
        }
    }
}

@Composable
fun AppNotificationBanner(viewModel: ChatViewModel) {
    val alerts by viewModel.systemNotifications.collectAsStateWithLifecycle()
    val lastAlert = alerts.firstOrNull { !it.isRead } ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (lastAlert.chatId != null) {
                        viewModel.openChatRoom(lastAlert.chatId)
                    }
                    viewModel.clearAllNotifications()
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Alert",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lastAlert.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lastAlert.content,
                        fontSize = 13.sp,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.clearAllNotifications() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close badge")
                }
            }
        }
    }
}

@Composable
fun UserAvatar(name: String, colorIndex: Int, status: String, size: Dp = 48.dp) {
    val palette = AvatarColorGradients.getOrElse(colorIndex) { AvatarColorGradients[0] }
    val cleanName = if (name.length >= 2) name.take(2).uppercase() else name.uppercase()

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Circle background avatar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(colors = listOf(palette.first, palette.second))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cleanName,
                color = Color.White,
                fontSize = (size.value * 0.35f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Online dot
        val dotColor = when (status) {
            "Online" -> ColorOnline
            "Busy" -> ColorBusy
            "Away" -> ColorAway
            else -> Color.Gray
        }

        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .clip(CircleShape)
                .background(dotColor)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}

@Composable
fun ChatListTab(viewModel: ChatViewModel) {
    val chats by viewModel.recentChats.collectAsStateWithLifecycle()
    val typingStatuses by viewModel.typingStatuses.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredChats = chats.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Group or Chat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text(
                text = "Chats",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversation...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No chats discovered.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the '+' button to discover contacts or form group chats.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredChats) { chat ->
                        val typingMessage = typingStatuses[chat.id]
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openChatRoom(chat.id) }
                                .testTag("chat_item_${chat.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Decide avatar color index
                                val isGroup = chat.isGroup
                                val avatarIndex = if (isGroup) 5 else (chat.id.hashCode() % 5).let { if (it < 0) -it else it }
                                val presenceStatus = if (isGroup) "Offline" else "Online"

                                UserAvatar(
                                    name = chat.name,
                                    colorIndex = avatarIndex,
                                    status = presenceStatus
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chat.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = formatTimestamp(chat.lastMessageTime),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = typingMessage ?: chat.lastMessageText,
                                        fontSize = 13.sp,
                                        fontWeight = if (typingMessage != null) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (typingMessage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateChatDialog(viewModel, onDismiss = { showCreateDialog = false })
    }
}

@Composable
fun CreateChatDialog(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    val friends by viewModel.friendsList.collectAsStateWithLifecycle()
    val availableUsers by viewModel.discoveredUsers.collectAsStateWithLifecycle()
    var customSearchQuery by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    LaunchedEffect(customSearchQuery) {
        viewModel.setUserSearchQuery(customSearchQuery)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Start Conversation",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Group name bar
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name (Optional for Group Chat)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Search user profiles
                OutlinedTextField(
                    value = customSearchQuery,
                    onValueChange = { customSearchQuery = it },
                    label = { Text("Search users to add...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "Select Participants:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Interactive users list
                Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display discovered all users
                        items(availableUsers) { usr ->
                            val isSelected = selectedMembers.contains(usr.id)
                            Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            if (isSelected) selectedMembers.remove(usr.id)
                                            else selectedMembers.add(usr.id)
                                        }
                                        .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(usr.displayName, usr.avatarColorIndex, usr.status, size = 32.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = usr.displayName,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selectedMembers.remove(usr.id)
                                        else selectedMembers.add(usr.id)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (selectedMembers.isEmpty()) return@Button
                            if (groupName.isNotEmpty()) {
                                viewModel.createGroupChat(groupName, selectedMembers)
                            } else {
                                viewModel.startDirectChat(selectedMembers.first())
                            }
                            onDismiss()
                        },
                        enabled = selectedMembers.isNotEmpty()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsTab(viewModel: ChatViewModel) {
    val friends by viewModel.friendsList.collectAsStateWithLifecycle()
    val pending by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val searchedUsers by viewModel.discoveredUsers.collectAsStateWithLifecycle()
    var searchEmailQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchEmailQuery) {
        viewModel.setUserSearchQuery(searchEmailQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Contacts",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Add client friend interface query
        OutlinedTextField(
            value = searchEmailQuery,
            onValueChange = { searchEmailQuery = it },
            placeholder = { Text("Find users by name/email...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Dynamic pending list
            if (pending.isNotEmpty()) {
                item {
                    Text("Pending Friend Requests (${pending.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(pending) { req ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = req.senderId, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Wants to connect with you", fontSize = 12.sp)
                            }
                            Row {
                                TextButton(onClick = { viewModel.acceptFriendRequest(req.id, false) }) {
                                    Text("Ignore", color = MaterialTheme.colorScheme.error)
                                }
                                Box(modifier = Modifier.width(4.dp))
                                Button(onClick = { viewModel.acceptFriendRequest(req.id, true) }) {
                                    Text("Accept")
                                }
                            }
                        }
                    }
                }
            }

            // Searched Discover Users List
            if (searchEmailQuery.isNotEmpty()) {
                item {
                    Text("Discovered Users", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(searchedUsers) { usr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(usr.displayName, usr.avatarColorIndex, usr.status)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(usr.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(usr.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = { viewModel.sendFriendRequest(usr.id) }) {
                            Text("Connect")
                        }
                    }
                }
            }

            // Existing Accepted Friends List
            item {
                Text("Your Connected Friends (${friends.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (friends.isEmpty()) {
                item {
                    Text("No connected contacts yet. Search above to invite friends!", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
                }
            } else {
                items(friends) { friend ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.startDirectChat(friend.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(friend.displayName, friend.avatarColorIndex, friend.status)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(friend.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(friend.bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "EnterChat",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageSearchTab(viewModel: ChatViewModel) {
    val query by viewModel.messageSearchQuery.collectAsStateWithLifecycle()
    val results by viewModel.messageSearchResults.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Search Messages",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setMessageSearchQuery(it) },
            placeholder = { Text("Type query text (e.g. commited, welcome, assistant)...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        if (query.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Find deep conversation transcripts securely across all historical backlogs.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(results) { res ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openChatRoom(res.chatId) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(res.senderName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(formatTimestamp(res.timestamp), fontSize = 11.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(res.text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: ChatViewModel) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val currentOverride by viewModel.darkThemeOverride.collectAsStateWithLifecycle()
    val isMuted by viewModel.soundSimulationEnabled.collectAsStateWithLifecycle()

    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editColorIndex by remember { mutableStateOf(0) }
    var expandedStatusMenu by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let {
            editName = it.displayName
            editBio = it.bio
            editColorIndex = it.avatarColorIndex
        }
    }

    val stateDisplay = user ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Profile & Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Current status row
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(stateDisplay.displayName, stateDisplay.avatarColorIndex, stateDisplay.status, size = 64.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stateDisplay.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Presence status: ${stateDisplay.status}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }

                Box {
                    Button(onClick = { expandedStatusMenu = true }) {
                        Text("Set Presence")
                    }

                    DropdownMenu(
                        expanded = expandedStatusMenu,
                        onDismissRequest = { expandedStatusMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🟢 Online") },
                            onClick = { viewModel.updateStatus("Online"); expandedStatusMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("🔴 Busy") },
                            onClick = { viewModel.updateStatus("Busy"); expandedStatusMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("🟡 Away") },
                            onClick = { viewModel.updateStatus("Away"); expandedStatusMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("⚪ Offline") },
                            onClick = { viewModel.updateStatus("Offline"); expandedStatusMenu = false }
                        )
                    }
                }
            }
        }

        // Edit Profile Details Category
        Text("Profile Identity", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))

        OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Display Name") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = editBio,
            onValueChange = { editBio = it },
            label = { Text("Biographical quote") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Select Avatar Color Row
        Text("Modify Avatar Color Accent", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(AvatarColorGradients) { index: Int, pair: Pair<Color, Color> ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(colors = listOf(pair.first, pair.second)))
                        .border(
                            width = if (editColorIndex == index) 3.dp else 0.dp,
                            color = if (editColorIndex == index) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { editColorIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    if (editColorIndex == index) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.updateProfile(editName, editBio, editColorIndex) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Profile Identity")
        }

        // App customization Settings Categories
        Text("App Preferences", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))

        // Sound theme toggle row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("In-Chat Ring Sound Alert", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Trigger virtual tones for incoming responses.", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = isMuted,
                    onCheckedChange = { viewModel.soundSimulationEnabled.value = it }
                )
            }
        }

        // Dark theme toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Dark Mode Override", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Enforce secure high-contrast dark theme canvas.", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = currentOverride == true,
                    onCheckedChange = { toggle ->
                        viewModel.darkThemeOverride.value = toggle
                    }
                )
            }
        }

        // Admin Access Gate
        if (stateDisplay.role == "ADMIN" || stateDisplay.email == "admin@bemessenger.com") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable { viewModel.navigateTo(Screen.ADMIN_DASHBOARD) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Access Administrative System", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Audit registered accounts, watch logs, delete messages.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Log out
        Button(
            onClick = { viewModel.logOut() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out Account", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
