// presentation/viewmodel/ChatListViewModel.kt
package com.astfreelancer.flowchat2.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astfreelancer.flowchat2.data.db.ChatWithPreview
import com.astfreelancer.flowchat2.data.db.MessageEntity
import com.astfreelancer.flowchat2.data.network.ApiService
import com.astfreelancer.flowchat2.data.network.NetworkResult
import com.astfreelancer.flowchat2.data.network.callAsFlow
import com.astfreelancer.flowchat2.data.network.model.HealthDto
import com.astfreelancer.flowchat2.data.repository.AuthRepository
import com.astfreelancer.flowchat2.data.repository.ChatRepository
import com.astfreelancer.flowchat2.data.repository.EventRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository,
    private val authRepository: AuthRepository,
    private val eventRepository: EventRepository,
    private val api: ApiService,
    private val myDeviceId: String
) : ViewModel() {

    val chats: StateFlow<List<ChatWithPreview>> = repository
        .chatListFlow(myDeviceId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearchQueryChange(newValue: String) {
        _searchQuery.value = newValue
    }

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible = _isSearchVisible.asStateFlow()

    val connectionState: StateFlow<NetworkResult<HealthDto>> =
        callAsFlow { api.health() }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                NetworkResult.Loading
            )

    fun toggleSearch() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) {
            _searchQuery.value = ""
        }
    }



    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val globalSearchResults: StateFlow<List<MessageEntity>> =
        searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.isEmpty() || it.length >= 2 }
            .flatMapLatest { query ->
                if (query.isEmpty()) flowOf(emptyList())
                else repository.searchAllMessagesFlow(query)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    init {
        viewModelScope.launch {
            authRepository.ensureDeviceRegistered()
            // после авторизации запускаем оба параллельно
            launch { eventRepository.syncOutbox() }
            launch { eventRepository.eventLoop().collect() }
        }
    }
}
