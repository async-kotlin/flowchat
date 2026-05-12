package com.astfreelancer.flowchat2.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

fun <T> callAsFlow(
    maxRetries: Int = 3,
    baseDelayMs: Long = 500L,
    retryHttp5xx: Boolean = true,
    block: suspend () -> T
): Flow<NetworkResult<T>> = flow {
    emit(NetworkResult.Loading)
    val data = block()
    emit(NetworkResult.Success(data))
}.retryWhen { cause, attempt ->
    if (cause is CancellationException) return@retryWhen false

    val http = cause as? HttpException
    val retriable = when {
        cause is IOException -> true
        http != null && http.code() in 500..599 -> retryHttp5xx
        else -> false
    }

    val moreTries = attempt < maxRetries
    if (retriable && moreTries) {
        delay(backoff(baseDelayMs, attempt))
        true
    } else {
        false
    }
}.catch { e ->
    if (e is CancellationException) throw e
    val http = e as? HttpException
    val code = http?.code()
    emit(NetworkResult.Error(e, code, e.message))
}
    .distinctUntilChanged()

private fun backoff(base: Long, attempt: Long): Long {
    val factor = 1L shl attempt.toInt()
    return min(base * factor, 4_000L)
}

