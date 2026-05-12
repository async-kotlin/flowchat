// data/db/ChatEntity.kt
package com.astfreelancer.flowchat2.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String, // UUID - идентификатор чата на сервере
    val peerName: String, // имя собеседника
    val peerAvatarUrl: String? = null // аватар собеседника
)
