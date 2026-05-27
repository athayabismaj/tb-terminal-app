package com.tbterminal.app.data.repository

import com.tbterminal.app.data.remote.NetworkResult

internal inline fun <T, R> NetworkResult<T>.toRepositoryResult(
    transform: (T) -> RepositoryResult<R>
): RepositoryResult<R> {
    return when (this) {
        is NetworkResult.Success -> transform(data)
        is NetworkResult.Error -> RepositoryResult.Error(code = code, message = message)
        is NetworkResult.Exception -> RepositoryResult.Exception(e)
    }
}
