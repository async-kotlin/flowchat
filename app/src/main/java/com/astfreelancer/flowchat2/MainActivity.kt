// MainActivity.kt
package com.astfreelancer.flowchat2

import com.astfreelancer.flowchat2.data.network.ApiService
import com.astfreelancer.flowchat2.data.network.NetworkModule
import com.astfreelancer.flowchat2.data.repository.AuthRepository
import com.astfreelancer.flowchat2.data.repository.EventRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.astfreelancer.flowchat2.data.db.AppDatabase
import com.astfreelancer.flowchat2.data.prefs.DeviceIdStore
import com.astfreelancer.flowchat2.data.prefs.dataStore
import com.astfreelancer.flowchat2.data.repository.ChatRepository
import com.astfreelancer.flowchat2.presentation.ui.AppNavGraph
import com.astfreelancer.flowchat2.presentation.ui.theme.FlowChat2Theme
import com.astfreelancer.flowchat2.presentation.viewmodel.ChatListViewModel
import com.astfreelancer.flowchat2.presentation.viewmodel.ChatListViewModelFactory
import kotlinx.coroutines.runBlocking
import kotlin.getValue

@Composable
fun DownloadButton() {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("Скачать") }

    Button(onClick = {
        scope.launch {
            text = "Загрузка..."
            delay(2000) // имитация скачивания
            text = "Готово!"
        }
    }) {
        Text(text)
    }
}


import android.os.Bundle

import androidx.lifecycle.viewModelScope

// MainActivity.kt
class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "flowchat.db"
        ).build()
    }

    private val deviceIdStore by lazy { DeviceIdStore(applicationContext.dataStore) }
    private val cachedDeviceId by lazy { runBlocking { deviceIdStore.getOrCreate() } }
    private val repository by lazy { ChatRepository(db) }

    private val okHttp by lazy {
        NetworkModule.createOkHttpClient(
            deviceIdProvider = { cachedDeviceId }
        )
    }
    private val retrofit by lazy { NetworkModule.createRetrofit(okHttp) }
    private val api by lazy { retrofit.create(ApiService::class.java) }
    private val authRepository by lazy { AuthRepository(api, deviceIdStore, db) }

    private val eventRepository by lazy { EventRepository(api, db, repository) }
    private val chatListViewModel: ChatListViewModel by viewModels {
        ChatListViewModelFactory(repository, authRepository, eventRepository, api, cachedDeviceId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowChat2Theme(darkTheme = false) {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    chatListViewModel = chatListViewModel,
                    repository = repository,
                    eventRepository = eventRepository,
                    api = api,
                    myDeviceId = cachedDeviceId
                )
            }
        }
    }
}
