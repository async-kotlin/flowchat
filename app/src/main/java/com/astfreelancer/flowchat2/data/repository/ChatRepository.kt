//data/repository/ChatRepository.kt
package com.astfreelancer.flowchat2.data.repository

import androidx.room.withTransaction
import com.astfreelancer.flowchat2.data.db.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach



class ChatRepository(private val db: AppDatabase) : IChatRepository {

    private val messageDao = db.messageDao()
    private val chatDao = db.chatDao()
    private val pendingOutboxDao = db.pendingOutboxDao()

    fun chatListFlow(myDeviceId: String): Flow<List<ChatWithPreview>> =
        chatDao.chatsWithPreview(myDeviceId)
            .onEach { delay(1000) }

    override fun messagesFlow(chatId: String): Flow<List<MessageEntity>> =
        messageDao.messagesByChat(chatId)

    fun pendingMessagesFlow(): Flow<List<PendingOutboxEntity>> =
        pendingOutboxDao.allPending()

    override suspend fun saveMessage(
        messageId: String,
        chatId: String,
        myDeviceId: String,
        text: String
    ) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            messageDao.upsert(
                MessageEntity(
                    id = messageId,
                    chatId = chatId,
                    senderId = myDeviceId,
                    text = text,
                    timestamp = now,
                    status = MessageStatus.PENDING
                )
            )
            pendingOutboxDao.insert(
                PendingOutboxEntity(
                    messageId = messageId,
                    createdAt = now
                )
            )
        }
    }

    suspend fun confirmDelivery(messageId: String) {
        db.withTransaction {
            messageDao.updateStatus(messageId, MessageStatus.SENT)
            pendingOutboxDao.deleteByMessageId(messageId)
        }
    }

    override suspend fun searchMessages(chatId: String, query: String): List<MessageEntity> =
        db.messageDao().searchMessages(chatId, query)

    fun searchAllMessagesFlow(query: String): Flow<List<MessageEntity>> =
        messageDao.searchAllMessages(query)

}