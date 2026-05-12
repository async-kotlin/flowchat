package com.astfreelancer.flowchat2.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

// data/repository/IEventRepository.kt

interface IEventRepository {
    val typingEvents: SharedFlow<Pair<String, String>>
    suspend fun syncOutbox()
    fun eventLoop(): Flow<Unit>
}