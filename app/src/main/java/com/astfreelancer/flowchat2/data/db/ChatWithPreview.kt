// data/db/ChatWithPreview.kt
package com.astfreelancer.flowchat2.data.db

data class ChatWithPreview(
    val id: String,
    val peerName: String,
    val peerAvatarUrl: String?,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long?,
    val unreadCount: Int
)
