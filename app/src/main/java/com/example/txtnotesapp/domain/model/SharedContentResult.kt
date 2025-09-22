package com.example.txtnotesapp.domain.model

sealed class SharedContentResult<out T> {
    data class Success<out T>(val data: T) : SharedContentResult<T>()
    data class Error(val message: String) : SharedContentResult<Nothing>()
}

fun <T, R> SharedContentResult<T>.map(transform: (T) -> R): SharedContentResult<R> {
    return when (this) {
        is SharedContentResult.Success -> SharedContentResult.Success(transform(data))
        is SharedContentResult.Error -> this
    }
}

fun <T> SharedContentResult<T>.onSuccess(action: (T) -> Unit): SharedContentResult<T> {
    if (this is SharedContentResult.Success) action(data)
    return this
}

fun <T> SharedContentResult<T>.onError(action: (String) -> Unit): SharedContentResult<T> {
    if (this is SharedContentResult.Error) action(message)
    return this
}