package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String, // email or generated uuid
    val displayName: String,
    val email: String,
    val passwordHash: String,
    val bio: String,
    val avatarColorIndex: Int, // index for avatar background colors
    val status: String, // "Online", "Offline", "Busy", "In a Meeting"
    val role: String, // "USER", "ADMIN"
    val isCurrentUser: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "friend_requests")
data class FriendRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val receiverId: String,
    val status: String // "PENDING", "ACCEPTED", "REJECTED"
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String, // unique chat uuid
    val name: String,
    val isGroup: Boolean,
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val createdBy: String
)

@Entity(tableName = "chat_members", primaryKeys = ["chatId", "userId"])
data class ChatMember(
    val chatId: String,
    val userId: String
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String, // unique message uuid
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileType: String = "NONE", // "NONE", "IMAGE", "DOCUMENT", "AUDIO", "VIDEO"
    val timestamp: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val chatId: String? = null,
    val isRead: Boolean = false
)
