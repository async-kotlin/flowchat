package com.astfreelancer.flowchat2.data.network

import com.astfreelancer.flowchat2.data.network.model.*
import retrofit2.http.*

interface ApiService {

    @GET("health")
    suspend fun health(): HealthDto

    @POST("auth/anonymous")
    suspend fun authAnonymous(@Body body: AnonymousAuthRequest): AnonymousAuthResponse

    @GET("chats")
    suspend fun chats(): ChatsResponse

    @POST("messages/send")
    suspend fun sendMessage(@Body body: SendMessageRequest): SendMessageResponse

    @GET("events")
    suspend fun events(@Query("since") since: Long): EventsResponse

    @POST("messages/{id}/ack")
    suspend fun ack(@Path("id") messageId: String)

    @POST("messages/{id}/read")
    suspend fun markRead(@Path("id") messageId: String)

    @POST("typing")
    suspend fun sendTyping(@Body body: TypingRequest)
}
