package com.astfreelancer.flowchat2.data.network.model

data class SendMessageRequest(
    val id: String,
    val chatId: String,
    val text: String
)
data class SendMessageResponse(
    val id: String,
    val status: String,
    val timestamp: Long
)
