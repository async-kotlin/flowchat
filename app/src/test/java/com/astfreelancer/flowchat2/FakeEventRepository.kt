package com.astfreelancer.flowchat2

import com.astfreelancer.flowchat2.data.repository.EventRepository
import com.astfreelancer.flowchat2.data.repository.IEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.emptyFlow

// app/src/test/java/com/astfreelancer/flowchat2/FakeEventRepository.kt

class FakeEventRepository : IEventRepository {

    private val _typingEvents =
        MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 1)

    override val typingEvents: SharedFlow<Pair<String, String>> = _typingEvents

    suspend fun emitTyping(chatId: String, senderId: String) {
        _typingEvents.emit(chatId to senderId)
    }

    override suspend fun syncOutbox() { /* ничего */ }
    override fun eventLoop(): Flow<Unit> = emptyFlow()
}