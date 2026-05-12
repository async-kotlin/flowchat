//presentation/viewmodel/ChatViewModel.kt
package com.astfreelancer.flowchat2.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astfreelancer.flowchat2.data.db.MessageEntity
import com.astfreelancer.flowchat2.data.network.ApiService
import com.astfreelancer.flowchat2.data.network.model.TypingRequest
import com.astfreelancer.flowchat2.data.repository.ChatRepository
import com.astfreelancer.flowchat2.data.repository.EventRepository
import com.astfreelancer.flowchat2.data.repository.IChatRepository
import com.astfreelancer.flowchat2.data.repository.IEventRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.system.measureTimeMillis

data class ChatUiState(
    val messages: List<MessageEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isPeerTyping: Boolean = false
)

sealed interface ChatEvent {
    data class NewMessageReceived(
        val messageId: String,
        val chatId: String
    ) : ChatEvent
}

sealed class SearchState {
    data class Results(val items: List<MessageEntity>) : SearchState()
    data class Searching(val query: String) : SearchState()
}

class ChatViewModel(
    private val repository: IChatRepository,
    private val eventRepository: IEventRepository,
    private val api: ApiService,
    private val chatId: String,
    private val myDeviceId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ChatEvent> = _events

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearchQueryChange(newValue: String) {
        _searchQuery.value = newValue
    }

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible = _isSearchVisible.asStateFlow()

    private val _inputEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)

    fun onInputChanged(text: String) {
        // так как эмиттим не из suspend-функции
        _inputEvents.tryEmit(text)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val peerTyping: StateFlow<Boolean> =
        eventRepository.typingEvents
            .filter { (eventChatId, senderId) ->
                eventChatId == chatId && senderId != myDeviceId
            }
            .transformLatest{
                emit(true)       // сразу показываем индикатор
                delay(3000)   // ждем 3 секунды
                emit(false)      // убираем индикатор
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false
            )

    fun toggleSearch() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) {
            _searchQuery.value = ""
        }
    }


    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<SearchState> =
        searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.isEmpty() || it.length >= 2 }
            .mapLatest { query ->
                if (query.isEmpty()) SearchState.Results(emptyList())
                else repository.searchMessages(chatId, query)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                SearchState.Results(emptyList())
            )


    init {
        viewModelScope.launch {
            repository.messagesFlow(chatId)
                .collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
        }
        viewModelScope.launch {
            peerTyping.collect { isTyping ->
                _uiState.value = _uiState.value.copy(
                    isPeerTyping = isTyping
                )
            }
        }
        viewModelScope.launch {
            _inputEvents
                .filter { it.isNotEmpty() }
                .distinctUntilChanged()
                .collectLatest {
                    try {
                        api.sendTyping(TypingRequest(chatId))
                    } catch (_: Exception) { }
                    delay(3000)
                }
        }
    }

    fun sendMessage(text: String) {
        val messageId = UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.saveMessage(
                messageId = messageId,
                chatId = chatId,
                myDeviceId = myDeviceId,
                text = text
            )
        }
    }
}

