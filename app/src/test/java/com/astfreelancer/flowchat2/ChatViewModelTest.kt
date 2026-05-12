package com.astfreelancer.flowchat2

import app.cash.turbine.test
import com.astfreelancer.flowchat2.data.db.MessageEntity
import com.astfreelancer.flowchat2.data.db.MessageStatus
import com.astfreelancer.flowchat2.data.network.NetworkResult
import com.astfreelancer.flowchat2.data.network.callAsFlow
import com.astfreelancer.flowchat2.presentation.viewmodel.ChatViewModel
import com.astfreelancer.flowchat2.presentation.viewmodel.SearchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

// app/src/test/java/com/example/flowchat/ChatViewModelTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        fakeRepo: FakeChatRepository = FakeChatRepository(),
        fakeEventRepo: FakeEventRepository = FakeEventRepository(),
        fakeApi: FakeApiService = FakeApiService()
    ) = ChatViewModel(
        repository = fakeRepo,
        eventRepository = fakeEventRepo,
        api = fakeApi,
        chatId = "chat1",
        myDeviceId = "me"
    )



    @Test
    fun `uiState reflects messages from repository`() = runTest {
        val fakeRepo = FakeChatRepository()
        val viewModel = createViewModel(fakeRepo = fakeRepo)

        // фейк отдает список сообщений
        val msg = MessageEntity(
            "1", "chat1", "peer", "Привет", 1000L,
            MessageStatus.DELIVERED
        )
        fakeRepo.emitMessages(listOf(msg))

        // ViewModel обновила состояние
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(msg.text, viewModel.uiState.value.messages.first().text)
    }

    @Test
    fun `naive approach to searchResults`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        backgroundScope.launch(testDispatcher) {
            viewModel.searchResults.collect {}
        }

        viewModel.onSearchQueryChange("Привет")
        advanceTimeBy(400)

        assertTrue(viewModel.searchResults.value is SearchState.Searching)
    }

    @Test
    fun `typing indicator shows then hides after 3 seconds`() =
        runTest(testDispatcher) {
            val fakeEventRepo = FakeEventRepository()
            val viewModel = createViewModel(fakeEventRepo = fakeEventRepo)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isPeerTyping)

            fakeEventRepo.emitTyping("chat1", "peer")
            advanceTimeBy(100)
            assertTrue(viewModel.uiState.value.isPeerTyping)

            advanceTimeBy(3100)
            assertFalse(viewModel.uiState.value.isPeerTyping)
        }

    @Test
    fun `repeated typing event resets the timer`() =
        runTest(testDispatcher) {
            val fakeEventRepo = FakeEventRepository()
            val viewModel = createViewModel(fakeEventRepo = fakeEventRepo)

            advanceUntilIdle()

            fakeEventRepo.emitTyping("chat1", "peer")
            advanceTimeBy(2000) // 2 с из 3 — еще горит
            assertTrue(viewModel.uiState.value.isPeerTyping)

            // новое событие — таймер сбрасывается
            fakeEventRepo.emitTyping("chat1", "peer")
            advanceTimeBy(2000) // снова 2 с от нового события — все еще горит
            assertTrue(viewModel.uiState.value.isPeerTyping)

            advanceTimeBy(1100) // больше 3 с от последнего события
            assertFalse(viewModel.uiState.value.isPeerTyping)
        }

    @Test
    fun `searchResults transitions through Searching to Results`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            backgroundScope.launch(testDispatcher) {
                viewModel.searchResults.collect {}
            }

            viewModel.searchResults.test {
                awaitItem() // начальное значение Results(emptyList())

                viewModel.onSearchQueryChange("Привет")
                advanceTimeBy(400)
                assertTrue(awaitItem() is SearchState.Searching)

                advanceTimeBy(2100)
                assertTrue(awaitItem() is SearchState.Results)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `callAsFlow emits Loading then Success`() = runTest {
        val flow = callAsFlow { "result" }

        flow.test {
            assertTrue(awaitItem() is NetworkResult.Loading)
            val success = awaitItem() as NetworkResult.Success
            assertEquals("result", success.data)
            awaitComplete()
        }
    }
    @Test
    fun `callAsFlow emits Error on IOException after retries`() = runTest {
        var attempts = 0
        val flow = callAsFlow(maxRetries = 2, baseDelayMs = 100L) {
            attempts++
            throw IOException("no connection")
        }

        flow.test {
            assertTrue(awaitItem() is NetworkResult.Loading)
            val error = awaitItem() as NetworkResult.Error
            assertTrue(error.throwable is IOException)
            awaitComplete()
        }

        assertEquals(3, attempts) // 1 попытка + 2 повтора
    }
    @Test
    fun `callAsFlow does not retry 5xx when retryHttp5xx is false`() = runTest {
        var attempts = 0
        val flow = callAsFlow(retryHttp5xx = false) {
            attempts++
            val response = Response.error<String>(
                500,
                "".toResponseBody("text/plain".toMediaType())
            )
            throw HttpException(response)
        }

        flow.test {
            assertTrue(awaitItem() is NetworkResult.Loading)
            val error = awaitItem() as NetworkResult.Error
            val http = error.throwable as HttpException
            assertEquals(500, http.code())
            awaitComplete()
        }

        assertEquals(1, attempts) // только одна попытка, без повторов
    }


}
