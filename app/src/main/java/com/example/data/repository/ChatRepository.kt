package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.data.api.GeminiApiClient
import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepository private constructor(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "be_messenger_db"
    )
        .fallbackToDestructiveMigration()
        .build()

    private val userDao = db.userDao()
    private val friendDao = db.friendDao()
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()
    private val notificationDao = db.notificationDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Flow of current logged in user
    val currentUserFlow: Flow<User?> = userDao.getCurrentUserFlow()

    // Map of chatId -> Is Typing status
    private val _typingStatuses = MutableStateFlow<Map<String, String>>(emptyMap())
    val typingStatuses: StateFlow<Map<String, String>> = _typingStatuses.asStateFlow()

    companion object {
        @Volatile
        private var instance: ChatRepository? = null

        fun getInstance(context: Context): ChatRepository {
            return instance ?: synchronized(this) {
                instance ?: ChatRepository(context).also { instance = it }
            }
        }
    }

    init {
        // Run database initialization on startup
        repositoryScope.launch {
            try {
                prepopulateDatabase()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Pre-population failed", e)
            }
        }
    }

    private suspend fun prepopulateDatabase() {
        val userCount = userDao.getUserCount()
        if (userCount > 0) return

        // Create simulated active contacts
        val assistant = User(
            id = "be.assistant@example.com",
            displayName = "BE Assistant (AI)",
            email = "be.assistant@example.com",
            passwordHash = "bot",
            bio = "I'm a real-time smart assistant powered by Gemini 3.5 Flash! Ask me anything.",
            avatarColorIndex = 0, // Cyan gradient index
            status = "Online",
            role = "ADMIN"
        )

        val sarah = User(
            id = "sarah.jenkins@example.com",
            displayName = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            passwordHash = "sarah123",
            bio = "UX & Visual Designer at BE. Pixel perfection is my jam! 🎨",
            avatarColorIndex = 1, // Soft Pink index
            status = "Online",
            role = "USER"
        )

        val marcus = User(
            id = "marcus.chen@example.com",
            displayName = "Marcus Chen",
            email = "marcus.chen@example.com",
            passwordHash = "marcus123",
            bio = "Lead Development Engineer. I turn coffee into production-ready Kotlin code. ☕",
            avatarColorIndex = 2, // Amber index
            status = "In a Meeting",
            role = "USER"
        )

        val aisha = User(
            id = "aisha.diop@example.com",
            displayName = "Aisha Diop",
            email = "aisha.diop@example.com",
            passwordHash = "aisha123",
            bio = "Product Director @ BE. Building products people love.",
            avatarColorIndex = 3, // Emerald index
            status = "Busy",
            role = "USER"
        )

        // Insert contacts
        userDao.insertOrUpdateUser(assistant)
        userDao.insertOrUpdateUser(sarah)
        userDao.insertOrUpdateUser(marcus)
        userDao.insertOrUpdateUser(aisha)

        // Insert initial Admin User (optional overview user)
        val admin = User(
            id = "admin@bemessenger.com",
            displayName = "System Root Admin",
            email = "admin@bemessenger.com",
            passwordHash = "admin123",
            bio = "Head Administrator for Content Moderation and Analytics.",
            avatarColorIndex = 4, // Deep Red index
            status = "Online",
            role = "ADMIN"
        )
        userDao.insertOrUpdateUser(admin)

        // Welcome Notification
        val banner = AppNotification(
            title = "Welcome to BE Messenger!",
            content = "Log in or Create an account to begin chat discovery, file sharing, group calls, and conversations with BE Assistant.",
            chatId = null,
            isRead = false
        )
        notificationDao.insertNotification(banner)
    }

    // --- Authentication ---
    suspend fun registerUser(name: String, email: String, pass: String, bio: String, avatarColorIndex: Int): Boolean {
        // Check if user exists
        val existing = userDao.getUserById(email)
        if (existing != null) return false

        // Clear previous user active flag
        userDao.clearCurrentUserFlag()

        val newUser = User(
            id = email,
            displayName = name,
            email = email,
            passwordHash = pass,
            bio = bio.ifEmpty { "Hey there! I am using BE Messenger." },
            avatarColorIndex = avatarColorIndex,
            status = "Online",
            role = "USER",
            isCurrentUser = true
        )
        userDao.insertOrUpdateUser(newUser)

        // Generate some default friend requests and chats for this user to make it alive!
        setupInitialUserMockData(newUser)
        return true
    }

    suspend fun loginUser(email: String, pass: String): Boolean {
        val user = userDao.getUserById(email) ?: return false
        if (user.passwordHash == pass) {
            userDao.clearCurrentUserFlag()
            userDao.insertOrUpdateUser(user.copy(isCurrentUser = true, status = "Online"))
            return true
        }
        return false
    }

    suspend fun logout() {
        val current = userDao.getCurrentUser()
        if (current != null) {
            userDao.insertOrUpdateUser(current.copy(isCurrentUser = false, status = "Offline"))
        }
    }

    suspend fun updateProfile(name: String, bio: String, avatarIndex: Int) {
        val current = userDao.getCurrentUser() ?: return
        userDao.updateProfile(current.id, name, bio, avatarIndex)
    }

    suspend fun updateStatus(status: String) {
        val current = userDao.getCurrentUser() ?: return
        userDao.updateUserStatus(current.id, status)
    }

    private suspend fun setupInitialUserMockData(user: User) {
        // Automatically create friendship connections with Sarah, Marcus and Assistant
        friendDao.sendOrUpdateFriendRequest(FriendRequest(senderId = "sarah.jenkins@example.com", receiverId = user.id, status = "ACCEPTED"))
        friendDao.sendOrUpdateFriendRequest(FriendRequest(senderId = "marcus.chen@example.com", receiverId = user.id, status = "ACCEPTED"))
        friendDao.sendOrUpdateFriendRequest(FriendRequest(senderId = "be.assistant@example.com", receiverId = user.id, status = "ACCEPTED"))
        friendDao.sendOrUpdateFriendRequest(FriendRequest(senderId = "aisha.diop@example.com", receiverId = user.id, status = "PENDING")) // Friend Invitation

        // Create 1-to-1 Chat Rooms
        val assistantChatId = "chat_assist_${user.id.hashCode()}"
        val sarahChatId = "chat_sarah_${user.id.hashCode()}"
        val marcusChatId = "chat_marcus_${user.id.hashCode()}"

        chatDao.insertChat(Chat(id = assistantChatId, name = "BE Assistant (AI)", isGroup = false, createdBy = "be.assistant@example.com"))
        chatDao.insertChatMembers(listOf(ChatMember(assistantChatId, user.id), ChatMember(assistantChatId, "be.assistant@example.com")))

        chatDao.insertChat(Chat(id = sarahChatId, name = "Sarah Jenkins", isGroup = false, createdBy = "sarah.jenkins@example.com"))
        chatDao.insertChatMembers(listOf(ChatMember(sarahChatId, user.id), ChatMember(sarahChatId, "sarah.jenkins@example.com")))

        chatDao.insertChat(Chat(id = marcusChatId, name = "Marcus Chen", isGroup = false, createdBy = "marcus.chen@example.com"))
        chatDao.insertChatMembers(listOf(ChatMember(marcusChatId, user.id), ChatMember(marcusChatId, "marcus.chen@example.com")))

        // Create group chat
        val devTeamChatId = "chat_group_dev_${user.id.hashCode()}"
        chatDao.insertChat(Chat(id = devTeamChatId, name = "BE Engineering Team", isGroup = true, createdBy = "marcus.chen@example.com"))
        chatDao.insertChatMembers(listOf(
            ChatMember(devTeamChatId, user.id),
            ChatMember(devTeamChatId, "marcus.chen@example.com"),
            ChatMember(devTeamChatId, "sarah.jenkins@example.com")
        ))

        // Prepopulate first messages
        insertInitialMessage(assistantChatId, "be.assistant@example.com", "BE Assistant (AI)", "Hi ${user.displayName}! Welcome to BE Messenger! I'm powered by real-time Gemini AI. Ask me any design or code question, or say help.")
        insertInitialMessage(sarahChatId, "sarah.jenkins@example.com", "Sarah Jenkins", "Hey ${user.displayName}! Welcome to our visual engineering chat. Are we starting on the new UI styles today? 🚀")
        insertInitialMessage(marcusChatId, "marcus.chen@example.com", "Marcus Chen", "Welcome aboard! Let me know when you've checked out the repository. Let's build something epic.")
        
        insertInitialMessage(devTeamChatId, "marcus.chen@example.com", "Marcus Chen", "Team, welcome ${user.displayName} to BE Messenger workspace group!")
        insertInitialMessage(devTeamChatId, "sarah.jenkins@example.com", "Sarah Jenkins", "Welcome! Excited to collaborate here.")

        // Welcoming Notification
        notificationDao.insertNotification(
            AppNotification(
                title = "Connected!",
                content = "You are now connected with BE Assistant, Sarah Jenkins, and Marcus Chen.",
                chatId = sarahChatId
            )
        )
    }

    private suspend fun insertInitialMessage(chatId: String, senderId: String, senderName: String, text: String) {
        val msg = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis() - 60000 // slightly in the past
        )
        messageDao.insertMessage(msg)
        chatDao.updateLastMessage(chatId, text, msg.timestamp)
    }

    // --- Search & Contacts ---
    fun searchUsers(query: String): Flow<List<User>> {
        return if (query.isEmpty()) {
            userDao.getAllOtherUsersFlow()
        } else {
            userDao.searchUsersFlow(query)
        }
    }

    fun getPendingRequests(userId: String): Flow<List<FriendRequest>> {
        return friendDao.getPendingRequestsFlow(userId)
    }

    fun getFriends(userId: String): Flow<List<User>> {
        return friendDao.getFriendshipsFlow(userId).map { friendships ->
            val list = mutableListOf<User>()
            friendships.forEach { fs ->
                val friendId = if (fs.senderId == userId) fs.receiverId else fs.senderId
                userDao.getUserById(friendId)?.let { list.add(it) }
            }
            list
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getUserById(userId: String): User? = userDao.getUserById(userId)

    suspend fun acceptFriendRequest(requestId: Int, status: String) {
        val current = friendDao.getPendingRequestsFlow(userDao.getCurrentUser()?.id ?: "").firstOrNull()?.find { it.id == requestId }
        if (current != null) {
            friendDao.sendOrUpdateFriendRequest(current.copy(status = status))
            if (status == "ACCEPTED") {
                // Pre-create chat
                val user = userDao.getCurrentUser() ?: return
                val friend = userDao.getUserById(current.senderId) ?: return
                val chatId = "chat_gen_${UUID.randomUUID().toString().take(6)}"
                chatDao.insertChat(Chat(id = chatId, name = friend.displayName, isGroup = false, createdBy = friend.id))
                chatDao.insertChatMembers(listOf(ChatMember(chatId, user.id), ChatMember(chatId, friend.id)))
                insertInitialMessage(chatId, friend.id, friend.displayName, "Awesome! We're now connected on BE Messenger.")
            }
        }
    }

    suspend fun sendFriendRequest(targetUserId: String) {
        val current = userDao.getCurrentUser() ?: return
        friendDao.sendOrUpdateFriendRequest(FriendRequest(senderId = current.id, receiverId = targetUserId, status = "PENDING"))
        notificationDao.insertNotification(
            AppNotification(
                title = "Friend Invitation Sent",
                content = "Your request to connect has been dispatched."
            )
        )
    }

    // --- Chats ---
    fun getChatsForUser(userId: String): Flow<List<Chat>> = chatDao.getChatsForUserFlow(userId)

    fun getChatByIdFlow(chatId: String): Flow<Chat?> = chatDao.getChatByIdFlow(chatId)

    suspend fun createGroupChat(name: String, selectedUserIds: List<String>) {
        val current = userDao.getCurrentUser() ?: return
        val chatId = "class_group_${UUID.randomUUID()}"
        
        // Save Chat header
        val header = Chat(id = chatId, name = name, isGroup = true, createdBy = current.id)
        chatDao.insertChat(header)

        // Save Members
        val members = mutableListOf<ChatMember>()
        members.add(ChatMember(chatId, current.id))
        selectedUserIds.forEach { uid ->
            members.add(ChatMember(chatId, uid))
        }
        chatDao.insertChatMembers(members)

        // System insert message
        val adminPrompt = "${current.displayName} created group '$name' with ${selectedUserIds.size} members."
        insertInitialMessage(chatId, "system", "System", adminPrompt)

        notificationDao.insertNotification(
            AppNotification(title = "New Group Created", content = "You created group '$name' successfully.", chatId = chatId)
        )
    }

    // --- Messages ---
    fun getMessagesForChat(chatId: String): Flow<List<Message>> = messageDao.getMessagesForChatFlow(chatId)

    fun searchMessages(query: String, chatId: String?): Flow<List<Message>> = messageDao.searchMessagesFlow(query, chatId)

    suspend fun sendMessage(
        chatId: String,
        text: String,
        fileUri: String? = null,
        fileType: String = "NONE",
        fileName: String? = null
    ) {
        val current = userDao.getCurrentUser() ?: return
        val msgId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // 1. Save user's message locally
        val userMsgText = if (fileType != "NONE") "📎 Sent an attachment: ${fileName ?: "file"}" else text
        val userMsg = Message(
            id = msgId,
            chatId = chatId,
            senderId = current.id,
            senderName = current.displayName,
            text = text,
            fileUri = fileUri,
            fileName = fileName,
            fileType = fileType,
            timestamp = timestamp,
            isSeen = false
        )
        messageDao.insertMessage(userMsg)
        chatDao.updateLastMessage(chatId, userMsgText, timestamp)

        // 2. Perform message simulation replies asynchronously
        repositoryScope.launch {
            handleAutoRepliesSimulated(chatId, text, fileType, current)
        }
    }

    private suspend fun handleAutoRepliesSimulated(chatId: String, text: String, fileType: String, currentUser: User) {
        // Query chat members to see who is on the other end
        val chat = chatDao.getChatById(chatId) ?: return
        val members = chatDao.getChatMembers(chatId).filter { it != currentUser.id }
        if (members.isEmpty()) return

        // Wait a short moment to show read receipt (simulate seen instantly for demo)
        delay(600)
        messageDao.markMessagesAsSeen(chatId, currentUser.id)

        // Determine who replies
        if (chat.isGroup) {
            // Group mode simulation
            delay(1000)
            val replierId = members.random()
            val replier = userDao.getUserById(replierId) ?: return

            // Show typing indicator
            _typingStatuses.update { it + (chatId to "${replier.displayName} is typing...") }
            delay(1500)
            _typingStatuses.update { it - chatId }

            val replyText = getGroupResponsePrompt(replier.displayName, text)
            val replyMsg = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = replier.id,
                senderName = replier.displayName,
                text = replyText,
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(replyMsg)
            chatDao.updateLastMessage(chatId, replyText, replyMsg.timestamp)

            // Local App Notification
            notificationDao.insertNotification(
                AppNotification(
                    title = "${chat.name} (${replier.displayName})",
                    content = replyText,
                    chatId = chatId
                )
            )
        } else {
            // 1-to-1 conversation simulation
            val targetUserId = members.first()
            val recipient = userDao.getUserById(targetUserId) ?: return

            // Show typing indicator
            _typingStatuses.update { it + (chatId to "${recipient.displayName} is typing...") }
            delay(1500)
            _typingStatuses.update { it - chatId }

            // Reply content
            val replyText: String = if (targetUserId == "be.assistant@example.com") {
                val systemPrompt = "You are BE Assistant, a helpful AI chat buddy embedded inside the 'BE Messenger' Android app. The current user's name is ${currentUser.displayName}. Respond in a friendly, conversational tone. Focus on speed, security, and helpful answers."
                GeminiApiClient.generateAIResponse(text, systemPrompt)
            } else {
                getFriendResponsePrompt(recipient.displayName, text, fileType)
            }

            val replyMsg = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = recipient.id,
                senderName = recipient.displayName,
                text = replyText,
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(replyMsg)
            chatDao.updateLastMessage(chatId, replyText, replyMsg.timestamp)

            // Dynamic notification triggers
            notificationDao.insertNotification(
                AppNotification(
                    title = recipient.displayName,
                    content = replyText.take(120),
                    chatId = chatId
                )
            )
        }
    }

    private fun getFriendResponsePrompt(friendName: String, userText: String, fileType: String): String {
        if (fileType != "NONE") {
            return "Wow, thank you for sharing that $fileType! It looks super high-fidelity. Let me save it to our conversation."
        }
        val lowerText = userText.lowercase()
        return when {
            lowerText.contains("dark mode") || lowerText.contains("theme") ->
                "Agreed! Go to Settings in BE Messenger and click the theme toggle. The entire application instantly matches Material Design rules."
            lowerText.contains("status") || lowerText.contains("online") ->
                "Yes! Look at my profile avatar, you'll see a green status light. Our presence indicator tracks online/offline real-time."
            lowerText.contains("group") || lowerText.contains("create") ->
                "Creating rooms is super clean. Tap the edit float button, select multiple contacts, name it, and we are connected!"
            lowerText.contains("hello") || lowerText.contains("hi") || lowerText.contains("hey") ->
                "Hi! Hope your day is going awesome. What are we building today on BE Messenger?"
            else ->
                "I completely agree. BE Messenger works incredibly fast thanks to Jetpack Compose! Let me know if you want to try mock file attachments."
        }
    }

    private fun getGroupResponsePrompt(memberName: String, userText: String): String {
        return "I'm on it! Let's discuss this task inside our developer forum. I'll write some Kotlin tests to verify."
    }

    // --- Notifications ---
    fun getNotifications(): Flow<List<AppNotification>> = notificationDao.getAllNotificationsFlow()

    suspend fun markNotificationRead(id: Int) = notificationDao.markAsRead(id)

    suspend fun clearNotifications() = notificationDao.deleteAllNotifications()

    // --- Admin Dashboard (Cross-User monitoring) ---
    // Expose ALL messages and users for system oversight.
    @androidx.room.Query("SELECT * FROM users")
    fun monitorAllUsersFlow(): Flow<List<User>> = db.userDao().getAllOtherUsersFlow() // we can expand or customize

    fun getAdminMessagesFlow(): Flow<List<Message>> {
        // Return latest messages across all chats
        return messageDao.searchMessagesFlow("", null)
    }

    suspend fun deleteMessageAdmin(messageId: String) {
        // Delete message if offensive, etc.
        // For simplicity we could just delete or replace text with flagged message
        val msg = messageDao.searchMessagesFlow("", null).firstOrNull()?.find { it.id == messageId }
        if (msg != null) {
            messageDao.insertMessage(msg.copy(text = "🚨 [This content was flagged and removed by Admin]"))
        }
    }

    suspend fun toggleUserRoleAdmin(userId: String) {
        val user = userDao.getUserById(userId) ?: return
        val newRole = if (user.role == "ADMIN") "USER" else "ADMIN"
        userDao.insertOrUpdateUser(user.copy(role = newRole))
    }
}
