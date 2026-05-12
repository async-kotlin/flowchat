package com.astfreelancer.flowchat2.data.network.model

data class EventsResponse(
    val events: List<EventDto>,
    val serverTime: Long
)
data class EventDto(
    val type: String,                         // "new_message", "status_update", "typing"
    val message: IncomingMessageDto? = null,  // только для new_message
    val messageId: String? = null,            // только для status_update
    val status: String? = null,               // только для status_update
    val chatId: String? = null,               // только для typing
    val senderId: String? = null              // только для typing
)
data class IncomingMessageDto(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long
)
