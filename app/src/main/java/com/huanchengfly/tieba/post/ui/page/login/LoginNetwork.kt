package com.huanchengfly.tieba.post.ui.page.login

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Suppress("DEPRECATION")
internal fun isLoginDefinitelyOffline(context: Context): Boolean {
    // An unvalidated network, VPN or a route currently changing can still reach Passport.
    return try {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        manager.activeNetwork == null && manager.allNetworks.isEmpty()
    } catch (_: RuntimeException) {
        false
    }
}

internal enum class LoginAttempt { Resume, OpenPage, CheckSession, Verify, Confirm, ChangeAccount }

internal class LoginNetwork(private val disconnected: () -> Boolean) {
    var pending by mutableStateOf<LoginAttempt?>(null)
        private set

    fun enter() {
        if (pending == null && disconnected()) pending = LoginAttempt.Resume
    }

    fun allow(attempt: LoginAttempt): Boolean {
        pending = attempt.takeIf { disconnected() }
        return pending == null
    }
}
