package com.avd.data.remote.sealed

sealed class ApiState<out T> {
    object Idle : ApiState<Nothing>()
    object Loading : ApiState<Nothing>()  // Loading state
    data class Success<out T>(val data: T) : ApiState<T>()  // Success with data
    data class Error(val message: String) : ApiState<Nothing>()  // Error with message
}