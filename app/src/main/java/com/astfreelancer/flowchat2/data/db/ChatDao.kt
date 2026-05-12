package com.astfreelancer.flowchat2.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("""
        SELECT
            c.id,
            c.peerName,
            c.peerAvatarUrl,
            m.text       AS lastMessageText,
            m.timestamp  AS lastMessageTimestamp,
            COALESCE(u.cnt, 0) AS unreadCount
        FROM chats c
        LEFT JOIN messages m ON m.id = (
            SELECT id FROM messages
            WHERE chatId = c.id
            ORDER BY timestamp DESC
            LIMIT 1
        )
        LEFT JOIN (
            SELECT chatId, COUNT(*) AS cnt
            FROM messages
            WHERE senderId != :myDeviceId
              AND status != 'READ'
            GROUP BY chatId
        ) u ON u.chatId = c.id
        ORDER BY m.timestamp DESC
    """)
    fun chatsWithPreview(myDeviceId: String): Flow<List<ChatWithPreview>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(chat: ChatEntity)

    @Query("UPDATE chats SET peerName = :peerName WHERE id = :id")
    suspend fun updatePeerName(id: String, peerName: String)

    @Transaction
    suspend fun upsertSafe(chat: ChatEntity) {
        insert(chat)
        updatePeerName(chat.id, chat.peerName)
    }

}
