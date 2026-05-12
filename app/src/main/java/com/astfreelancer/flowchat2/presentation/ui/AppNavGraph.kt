// presentation/ui/AppNavGraph.kt
package com.astfreelancer.flowchat2.presentation.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.astfreelancer.flowchat2.data.network.ApiService
import com.astfreelancer.flowchat2.data.repository.ChatRepository
import com.astfreelancer.flowchat2.data.repository.EventRepository
import com.astfreelancer.flowchat2.presentation.viewmodel.ChatListViewModel
import com.astfreelancer.flowchat2.presentation.viewmodel.ChatViewModel
import com.astfreelancer.flowchat2.presentation.viewmodel.ChatViewModelFactory

@Composable
fun AppNavGraph(
    navController: NavHostController,
    chatListViewModel: ChatListViewModel,
    repository: ChatRepository,
    eventRepository: EventRepository,
    api: ApiService,
    myDeviceId: String
) {
    NavHost(
        navController = navController,
        startDestination = "chat_list"
    ) {
        composable("chat_list") {
            ChatListScreen(
                viewModel = chatListViewModel,
                onChatClick = { chatId, peerName ->
                    navController.navigate("chat/$chatId/$peerName")
                }
            )
        }

        composable(
            route = "chat/{chatId}/{peerName}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("peerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
                ?: return@composable
            val peerName = backStackEntry.arguments?.getString("peerName")
                ?: "Чат"
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(repository, eventRepository, api, chatId, myDeviceId)
            )
            ChatScreen(
                viewModel = chatViewModel,
                myDeviceId = myDeviceId,
                peerName = peerName,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}