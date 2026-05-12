// presentation/viewmodel/ChatListViewModelFactory.kt
package com.astfreelancer.flowchat2.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.astfreelancer.flowchat2.data.network.ApiService
import com.astfreelancer.flowchat2.data.repository.AuthRepository
import com.astfreelancer.flowchat2.data.repository.ChatRepository
import com.astfreelancer.flowchat2.data.repository.EventRepository

class ChatListViewModelFactory(
    private val repository: ChatRepository,
    private val authRepository: AuthRepository,
    private val eventRepository: EventRepository,
    private val api: ApiService, // добавили
    private val myDeviceId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatListViewModel(repository, authRepository, eventRepository, api, myDeviceId) as T
}
