package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<User?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: String): Flow<User?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 0")
    fun getAllOtherUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE isCurrentUser = 0 AND (displayName LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%')")
    fun searchUsersFlow(query: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: User)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String)

    @Query("UPDATE users SET displayName = :name, bio = :bio, avatarColorIndex = :colorIndex WHERE id = :userId")
    suspend fun updateProfile(userId: String, name: String, bio: String, colorIndex: Int)

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUserFlag()

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Dao
interface FriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sendOrUpdateFriendRequest(request: FriendRequest)

    @Query("""
        SELECT * FROM friend_requests 
        WHERE receiverId = :userId AND status = 'PENDING'
    """)
    fun getPendingRequestsFlow(userId: String): Flow<List<FriendRequest>>

    @Query("""
        SELECT * FROM friend_requests 
        WHERE (senderId = :userId OR receiverId = :userId) AND status = 'ACCEPTED'
    """)
    fun getFriendshipsFlow(userId: String): Flow<List<FriendRequest>>
}

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMembers(members: List<ChatMember>)

    @Query("""
        SELECT * FROM chats 
        WHERE id IN (SELECT chatId FROM chat_members WHERE userId = :userId)
        ORDER BY lastMessageTime DESC
    """)
    fun getChatsForUserFlow(userId: String): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): Chat?

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatByIdFlow(chatId: String): Flow<Chat?>

    @Query("UPDATE chats SET lastMessageText = :text, lastMessageTime = :timestamp WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, text: String, timestamp: Long)

    @Query("SELECT userId FROM chat_members WHERE chatId = :chatId")
    fun getChatMembersFlow(chatId: String): Flow<List<String>>

    @Query("SELECT userId FROM chat_members WHERE chatId = :chatId")
    suspend fun getChatMembers(chatId: String): List<String>
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForChat(chatId: String): Message?

    @Query("SELECT * FROM messages WHERE text LIKE '%' || :query || '%' AND (chatId = :chatId OR :chatId IS NULL) ORDER BY timestamp DESC")
    fun searchMessagesFlow(query: String, chatId: String?): Flow<List<Message>>

    @Query("UPDATE messages SET isSeen = 1 WHERE chatId = :chatId AND senderId != :currentUserId")
    suspend fun markMessagesAsSeen(chatId: String, currentUserId: String)
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<AppNotification>>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}

@Database(
    entities = [
        User::class,
        FriendRequest::class,
        Chat::class,
        ChatMember::class,
        Message::class,
        AppNotification::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun friendDao(): FriendDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun notificationDao(): NotificationDao
}
