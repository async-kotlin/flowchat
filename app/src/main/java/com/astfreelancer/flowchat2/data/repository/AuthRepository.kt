package com.astfreelancer.flowchat2.data.repository

import android.util.Log
import com.astfreelancer.flowchat2.data.db.AppDatabase
import com.astfreelancer.flowchat2.data.db.ChatEntity
import com.astfreelancer.flowchat2.data.network.ApiService
import com.astfreelancer.flowchat2.data.network.model.AnonymousAuthRequest
import com.astfreelancer.flowchat2.data.prefs.DeviceIdStore

// data/repository/AuthRepository.kt
class AuthRepository(
    private val api: ApiService,
    private val deviceIdStore: DeviceIdStore,
    private val db: AppDatabase
) {
    suspend fun ensureDeviceRegistered(): Boolean {
        val id = deviceIdStore.getOrCreate()

        return runCatching { api.authAnonymous(AnonymousAuthRequest(id)) }
            .onSuccess { response ->
                response.chats.forEach { chat ->
                    db.chatDao().upsertSafe(
                        ChatEntity(id = chat.id, peerName = chat.peerName)
                    )
                }
                Log.i("AuthRepository", "auth OK, deviceId=$id, chats=${response.chats.size}")
                response.chats.forEach { chat ->
                    Log.i("AuthRepository", "chat: id=${chat.id}, peer=${chat.peerName}")
                }
            }
            .onFailure { e ->
                Log.w("AuthRepository", "auth FAILED: ${e.message}")
            }
            .isSuccess
    }
}