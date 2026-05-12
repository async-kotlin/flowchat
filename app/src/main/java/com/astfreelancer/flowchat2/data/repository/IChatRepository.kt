package com.astfreelancer.flowchat2.data.repository

import com.astfreelancer.flowchat2.data.db.MessageEntity
import kotlinx.coroutines.flow.Flow

// data/repository/IChatRepository.kt
interface IChatRepository {
    fun messagesFlow(chatId: String): Flow<List<MessageEntity>>
    suspend fun searchMessages(chatId: String, query: String): List<MessageEntity>
    suspend fun saveMessage(
        messageId: String, chatId: String,
        myDeviceId: String, text: String
    )
}