package com.mvvmmovie.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Answers the ACCESS_NETWORK_STATE question the manifest has always asked for: it lets a
 * request fail fast with "you are offline" instead of surfacing a DNS error to the user.
 */
interface NetworkMonitor {

    fun isOnline(): Boolean
}

@Singleton
class AndroidNetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context
) : NetworkMonitor {

    /**
     * Deliberately does not require NET_CAPABILITY_VALIDATED: validation fails on networks that
     * block Android's connectivity check, behind some VPNs, and for a few seconds after joining
     * Wi-Fi, all while TMDB is reachable. A network that only claims internet is let through,
     * and a request that then fails surfaces as an ordinary connection error.
     */
    override fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService<ConnectivityManager>() ?: return true
        val capabilities = connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork ?: return false
        ) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/** Raised before a call is made, so callers can tell "offline" from a server failure. */
class OfflineException : java.io.IOException("No network connection")
