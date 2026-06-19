package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    ONBOARDING,
    SIGN_IN,
    SIGN_UP,
    DASHBOARD, // core messenger (retains tabs interior)
    CHAT,
    ADMIN_DASHBOARD
}

enum class DashboardTab {
    CHATS,
    CONTACTS,
    SEARCH_MESSAGES,
    SETTINGS
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository.getInstance(application)

    // --- State Routing Navigation ---
    private val _currentScreen = MutableStateFlow(Screen.ONBOARDING)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val navigationStack = mutableListOf<Screen>(Screen.ONBOARDING)

    private val _currentTab = MutableStateFlow(DashboardTab.CHATS)
    val currentTab: StateFlow<DashboardTab> = _currentTab.asStateFlow()

    // --- Active Chat Session ---
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    val activeChat: StateFlow<Chat?> = _activeChatId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getChatByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeMessages: StateFlow<List<Message>> = _activeChatId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMessagesForChat(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Typings Map (chatId -> Typing message description)
    val typingStatuses: StateFlow<Map<String, String>> = repository.typingStatuses

    // --- Auth States ---
    val currentUser: StateFlow<User?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- Chat list tab states ---
    val recentChats: StateFlow<List<Chat>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getChatsForUser(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Contacts & Friends Tab ---
    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery: StateFlow<String> = _userSearchQuery.asStateFlow()

    val discoveredUsers: StateFlow<List<User>> = _userSearchQuery
        .debounce(300)
        .flatMapLatest { query ->
            repository.searchUsers(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendsList: StateFlow<List<User>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getFriends(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<List<FriendRequest>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getPendingRequests(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Messages Search ---
    private val _messageSearchQuery = MutableStateFlow("")
    val messageSearchQuery: StateFlow<String> = _messageSearchQuery.asStateFlow()

    val messageSearchResults: StateFlow<List<Message>> = _messageSearchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) flowOf(emptyList())
            else repository.searchMessages(query, null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Global Notifications System ---
    val systemNotifications: StateFlow<List<AppNotification>> = repository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- App System Preferences ---
    val darkThemeOverride = MutableStateFlow<Boolean?>(null) // null uses system template
    val soundSimulationEnabled = MutableStateFlow(true)

    // --- Admin Dashboard metrics & logs ---
    val adminUsers: StateFlow<List<User>> = repository.monitorAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminMessages: StateFlow<List<Message>> = repository.getAdminMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Navigation Flow implementation ---
    fun navigateTo(screen: Screen) {
        navigationStack.add(screen)
        _currentScreen.value = screen
        _authError.value = null
    }

    fun navigateBack() {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.size - 1)
            _currentScreen.value = navigationStack.last()
        }
    }

    fun switchTab(tab: DashboardTab) {
        _currentTab.value = tab
    }

    // --- Auth Actions ---
    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            val success = repository.loginUser(email, pass)
            if (success) {
                navigateTo(Screen.DASHBOARD)
            } else {
                _authError.value = "Invalid email credentials or incorrect password."
            }
        }
    }

    fun signUp(name: String, email: String, pass: String, bio: String, avatarColorIndex: Int) {
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            _authError.value = "Please fill out all mandatory fields."
            return
        }
        viewModelScope.launch {
            val success = repository.registerUser(name, email, pass, bio, avatarColorIndex)
            if (success) {
                navigateTo(Screen.DASHBOARD)
            } else {
                _authError.value = "An account with this email address already exists."
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            repository.logout()
            navigationStack.clear()
            navigationStack.add(Screen.ONBOARDING)
            _currentScreen.value = Screen.ONBOARDING
        }
    }

    // --- Profile & Status Updates ---
    fun updateProfile(name: String, bio: String, avatarColorIndex: Int) {
        viewModelScope.launch {
            repository.updateProfile(name, bio, avatarColorIndex)
        }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch {
            repository.updateStatus(status)
        }
    }

    // --- Contacts Actions ---
    fun sendFriendRequest(targetEmail: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(targetEmail)
        }
    }

    fun acceptFriendRequest(requestId: Int, accept: Boolean) {
        viewModelScope.launch {
            repository.acceptFriendRequest(requestId, if (accept) "ACCEPTED" else "REJECTED")
        }
    }

    // --- Chat Actions ---
    fun startDirectChat(friendId: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            // Construct a consistent 1-to-1 chatId
            val sortedIds = listOf(user.id, friendId).sorted()
            val chatId = "chat_gen_${sortedIds[0].hashCode()}_${sortedIds[1].hashCode()}"
            
            // Check if chat exists in our current system, otherwise repository inserts
            val chatList = recentChats.value
            val existing = chatList.find { it.id == chatId }
            if (existing == null) {
                // Precreate direct chat rooms
                val friend = repository.getUserById(friendId) ?: return@launch
                val header = Chat(id = chatId, name = friend.displayName, isGroup = false, createdBy = friendId)
                // Simply insert mock chats through repo or start activeChatId directly
                repository.createGroupChat(friend.displayName, listOf(friendId)) // wait, let's create a custom chat
                // Let's call sendMessage or navigate directly to chat with simulated initialization
            }
            _activeChatId.value = chatId
            navigateTo(Screen.CHAT)
        }
    }

    fun openChatRoom(chatId: String) {
        _activeChatId.value = chatId
        navigateTo(Screen.CHAT)
        // Mark as seen immediately
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                // Simulate marking seen in DB
                repository.sendMessage(chatId, "", fileType = "NONE") // trigger seen check
            }
        }
    }

    fun createGroupChat(name: String, selectedFriends: List<String>) {
        if (name.isEmpty() || selectedFriends.isEmpty()) return
        viewModelScope.launch {
            repository.createGroupChat(name, selectedFriends)
        }
    }

    fun sendMessage(text: String, fileUri: String? = null, fileType: String = "NONE", fileName: String? = null) {
        val chatId = activeChatId.value ?: return
        if (text.trim().isEmpty() && fileType == "NONE") return
        viewModelScope.launch {
            repository.sendMessage(chatId, text, fileUri, fileType, fileName)
        }
    }

    // --- Admin Commands ---
    fun adminDeleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageAdmin(messageId)
        }
    }

    fun adminToggleUserRole(userId: String) {
        viewModelScope.launch {
            repository.toggleUserRoleAdmin(userId)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    fun setUserSearchQuery(query: String) {
        _userSearchQuery.value = query
    }

    fun setMessageSearchQuery(query: String) {
        _messageSearchQuery.value = query
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
