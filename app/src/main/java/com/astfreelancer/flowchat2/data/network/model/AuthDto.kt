package com.astfreelancer.flowchat2.data.network.model

data class AnonymousAuthRequest(val deviceId: String)
data class AnonymousAuthResponse(
    val deviceId: String,
    val chats: List<ChatDto>
)

