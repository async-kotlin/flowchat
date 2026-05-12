package com.astfreelancer.flowchat2

import com.astfreelancer.flowchat2.data.network.ApiService
import com.astfreelancer.flowchat2.data.network.model.*

// app/src/test/java/com/example/flowchat/FakeApiService.kt
class FakeApiService : ApiService {
    var typingCallCount = 0

    override suspend fun sendTyping(body: TypingRequest) {
        typingCallCount++
    }

    override suspend fun health(): HealthDto = HealthDto(ok = true)
    override suspend fun authAnonymous(body: AnonymousAuthRequest) =
        AnonymousAuthResponse(deviceId = body.deviceId, chats = emptyList())
    override suspend fun chats() = ChatsResponse(emptyList())
    override suspend fun sendMessage(body: SendMessageRequest) =
        SendMessageResponse(id = body.id, status = "SENT", timestamp = 0L)
    override suspend fun events(since: Long) =
        EventsResponse(events = emptyList(), serverTime = since)
    override suspend fun ack(messageId: String) { /* ничего */ }
    override suspend fun markRead(messageId: String) { /* ничего */ }
}
