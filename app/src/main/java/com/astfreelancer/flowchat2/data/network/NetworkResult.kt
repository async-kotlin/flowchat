package com.astfreelancer.flowchat2.data.network

sealed interface NetworkResult<out T> {
    data object Loading : NetworkResult<Nothing>
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Error(
        val throwable: Throwable,
        val code: Int? = null,
        val message: String? = null
    ) : NetworkResult<Nothing>
}
