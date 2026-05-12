// presentation/ui/ChatScreen.kt
package com.astfreelancer.flowchat2.presentation.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astfreelancer.flowchat2.data.db.MessageEntity
import com.astfreelancer.flowchat2.data.db.MessageStatus
import com.astfreelancer.flowchat2.presentation.viewmodel.ChatEvent
import com.astfreelancer.flowchat2.presentation.viewmodel.ChatViewModel
import com.astfreelancer.flowchat2.presentation.viewmodel.SearchState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    myDeviceId: String = "",

    // имя собеседника для отображения в верхней панели
    peerName: String = "Чат",

    // колбэк, вызываемый при нажатии кнопки "Назад"
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchVisible by viewModel.isSearchVisible.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val searchState by viewModel.searchResults.collectAsStateWithLifecycle()

    // Автоскролл при новом сообщении
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.NewMessageReceived -> {
                    // проиграть звук уведомления
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(peerName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleSearch() }
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            // Индикатор "печатает..."
            AnimatedVisibility(
                visible = state.isPeerTyping,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    "собеседник печатает...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            // Поисковая строка
            AnimatedVisibility(visible = isSearchVisible) {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        viewModel.onSearchQueryChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp, vertical = 4.dp
                        ),
                    placeholder = {
                        Text("Поиск по сообщениям...")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.toggleSearch() }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription =
                                    "Закрыть поиск"
                            )
                        }
                    }
                )
            }
            if (state.isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (isSearchVisible && searchQuery.isNotEmpty()) {
                when (val s = searchState) {
                    is SearchState.Searching -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ищем '${s.query}'...")
                        }
                    }

                    is SearchState.Results -> {
                        if (s.items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Ничего не найдено")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                                items(s.items, key = { it.id }) { msg ->
                                    MessageBubble(
                                        message = msg,
                                        isMine = msg.senderId == myDeviceId
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(state.messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            isMine = msg.senderId == myDeviceId
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            MessageInput(
                onSend = { text -> viewModel.sendMessage(text) },
                onInputChanged = { text -> viewModel.onInputChanged(text) }
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR2)
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun MessageBubble(message: MessageEntity, isMine: Boolean) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val bubbleColor = if (isMine)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    else
        Color(0xFFE3E8F0)

    val textColor = if (isMine)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    val timeFormat = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End
        else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = screenWidth * 0.75f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f)
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StatusIcon(message.status)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInput(
    onSend: (String) -> Unit,
    onInputChanged: (String) -> Unit = {}
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = {
                text = it
                onInputChanged(it)
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Сообщение...") },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            maxLines = 4
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text.trim())
                    text = ""
                }
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Отправить",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun StatusIcon(status: MessageStatus, modifier: Modifier = Modifier) {
    val (text, color) = when (status) {
        MessageStatus.PENDING -> "\u23F3" to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.SENT -> "\u2713" to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.DELIVERED -> "\u2713\u2713" to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.READ -> "\u2713\u2713" to Color(0xFF4FC3F7)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}
