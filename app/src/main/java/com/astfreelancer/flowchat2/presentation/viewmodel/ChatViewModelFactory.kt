package com.astfreelancer.flowchat2.presentation.viewmodel

// presentation/viewmodel/ChatViewModelFactory.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.astfreelancer.flowchat2.data.network.ApiService
import com.astfreelancer.flowchat2.data.repository.ChatRepository
import com.astfreelancer.flowchat2.data.repository.EventRepository

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val eventRepository: EventRepository,
    private val api: ApiService,
    private val chatId: String,
    private val myDeviceId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatViewModel(repository, eventRepository, api, chatId, myDeviceId) as T
}
