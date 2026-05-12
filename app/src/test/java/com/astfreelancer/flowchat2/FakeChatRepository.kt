package com.astfreelancer.flowchat2

import com.astfreelancer.flowchat2.data.db.MessageEntity
import com.astfreelancer.flowchat2.data.repository.ChatRepository
import com.astfreelancer.flowchat2.data.repository.IChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeChatRepository : IChatRepository {
    private val _messages =
        MutableStateFlow<List<MessageEntity>>(emptyList())

    fun emitMessages(list: List<MessageEntity>) {
        _messages.value = list
    }

    override fun messagesFlow(chatId: String): Flow<List<MessageEntity>> =
        _messages

    override suspend fun searchMessages(
        chatId: String, query: String
    ): List<MessageEntity> = emptyList()

    override suspend fun saveMessage(
        messageId: String, chatId: String,
        myDeviceId: String, text: String
    ) { /* ничего */ }
}