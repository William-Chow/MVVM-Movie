package com.mvvmmovie.viewmodel

import androidx.annotation.StringRes
import com.mvvmmovie.R
import com.mvvmmovie.network.OfflineException
import retrofit2.HttpException
import java.io.IOException

/**
 * Why a request failed, at the level the screen can explain it. Raw exception text such as
 * "HTTP 401 " or "Unable to resolve host" never reaches the user.
 */
enum class LoadError(@param:StringRes val messageRes: Int) {

    /** No usable network, detected before the request was made. */
    OFFLINE(R.string.error_no_network),

    /** The request was attempted but the connection failed or timed out. */
    NETWORK(R.string.error_network),

    /** TMDB refused the API key. */
    AUTH(R.string.error_auth),

    /** TMDB answered with a 5xx. */
    SERVER(R.string.error_server),

    UNKNOWN(R.string.error_unknown)
}

fun Throwable.toLoadError(): LoadError = when (this) {
    is OfflineException -> LoadError.OFFLINE
    is IOException -> LoadError.NETWORK
    is HttpException -> when (code()) {
        401, 403 -> LoadError.AUTH
        in 500..599 -> LoadError.SERVER
        else -> LoadError.UNKNOWN
    }
    else -> LoadError.UNKNOWN
}
