// data/repository/EventRepository.kt
package com.astfreelancer.flowchat2.data.repository

import android.util.Log
import com.astfreelancer.flowchat2.data.db.AppDatabase
import com.astfreelancer.flowchat2.data.db.MessageEntity
import com.astfreelancer.flowchat2.data.db.MessageStatus
import com.astfreelancer.flowchat2.data.network.ApiService
import com.astfreelancer.flowchat2.data.network.model.EventsResponse
import com.astfreelancer.flowchat2.data.network.model.SendMessageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlin.math.min

class EventRepository(
    private val api: ApiService,
    private val db: AppDatabase,
    private val chatRepository: ChatRepository
) : IEventRepository {
    private var lastServerTime = 0L
    private val _typingEvents = MutableSharedFlow<Pair<String, String>>(
        extraBufferCapacity = 1
    )
    override val typingEvents: SharedFlow<Pair<String, String>> = _typingEvents

    override suspend fun syncOutbox() {
        chatRepository.pendingMessagesFlow().collect { entries ->
            entries.forEach { entry ->
                try {
                    val message = db.messageDao().getById(entry.messageId)
                        ?: return@forEach

                    api.sendMessage(
                        SendMessageRequest(
                            id = message.id,
                            chatId = message.chatId,
                            text = message.text
                        )
                    )
                    chatRepository.confirmDelivery(message.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("Outbox", "Failed to send ${entry.messageId}: ${e.message}")
                }
            }
        }
    }


    override fun eventLoop(): Flow<Unit> = flow {
        while (true) {
            val response = api.events(lastServerTime)
            processEvents(response)
            lastServerTime = response.serverTime
            emit(Unit) // имитация эмита
        }
    }
        .retryWhen { cause, attempt ->
            if (cause is CancellationException) return@retryWhen false
            val delay = min(1000L * (1L shl attempt.toInt()), 30_000L)
            Log.w(
                "EventRepository",
                "Polling error, retry #$attempt in ${delay}ms: ${cause.message}"
            )
            delay(delay)
            true
        }

    private suspend fun processEvents(response: EventsResponse) {
        response.events.forEach { event ->
            when (event.type) {
                "new_message" -> {
                    event.message?.let { msg ->
                        db.messageDao().upsert(
                            MessageEntity(
                                id = msg.id,
                                chatId = msg.chatId,
                                senderId = msg.senderId,
                                text = msg.text,
                                timestamp = msg.timestamp,
                                status = MessageStatus.DELIVERED
                            )
                        )
                        // подтверждаем доставку для сервера
                        api.ack(msg.id)
                    }
                }
                "status_update" -> {
                    val msgId = event.messageId ?: return@forEach
                    val newStatus = event.status ?: return@forEach
                    db.messageDao().updateStatus(
                        msgId,
                        MessageStatus.valueOf(newStatus.uppercase())
                    )
                }
                "typing" -> {
                    val chatId = event.chatId ?: return@forEach
                    val senderId = event.senderId ?: return@forEach
                    _typingEvents.emit(chatId to senderId)
                }
            }
        }
    }
}
