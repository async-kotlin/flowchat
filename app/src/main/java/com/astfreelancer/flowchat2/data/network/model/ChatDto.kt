package com.astfreelancer.flowchat2.data.network.model

data class ChatsResponse(val chats: List<ChatDto>)
data class ChatDto(val id: String, val peerName: String)
