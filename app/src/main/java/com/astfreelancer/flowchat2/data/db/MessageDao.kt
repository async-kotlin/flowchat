package com.astfreelancer.flowchat2.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun messagesByChat(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: String, status: MessageStatus)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getById(messageId: String): MessageEntity?

    @Query(
        """
        SELECT * FROM messages 
        WHERE chatId = :chatId 
          AND text LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        """
    )
    suspend fun searchMessages(chatId: String, query: String): List<MessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE text LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        LIMIT 50
        """
    )
    fun searchAllMessages(query: String): Flow<List<MessageEntity>>
}
