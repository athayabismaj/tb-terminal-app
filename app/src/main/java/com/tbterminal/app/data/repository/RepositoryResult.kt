package com.tbterminal.app.data.repository

sealed interface RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>
    data class Error(val code: String, val message: String) : RepositoryResult<Nothing>
    data class Exception(val throwable: Throwable) : RepositoryResult<Nothing>
}
