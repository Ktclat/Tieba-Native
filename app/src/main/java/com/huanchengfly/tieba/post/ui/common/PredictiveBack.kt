package com.huanchengfly.tieba.post.ui.common

import android.os.Build
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.booleanPreferencesKey

internal val PredictiveBackKey = booleanPreferencesKey("predictiveBack")

/** Keep a single navigation stack and let its own handlers decide what Back means. */
@Composable
internal fun ProvideBackDispatcher(predictive: Boolean, content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        content()
        return
    }

    val parent = checkNotNull(LocalOnBackPressedDispatcherOwner.current)
    val commitOnly = remember(parent) {
        object : OnBackPressedDispatcherOwner {
            override val lifecycle = parent.lifecycle
            override val onBackPressedDispatcher = OnBackPressedDispatcher()
        }
    }
    if (!predictive) {
        DisposableEffect(parent, commitOnly) {
            // This callback deliberately receives only the completed gesture. NavHost,
            // tab handlers and editors still get the same normal Back dispatch order.
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (commitOnly.onBackPressedDispatcher.hasEnabledCallbacks()) {
                        commitOnly.onBackPressedDispatcher.onBackPressed()
                    } else {
                        // At the root, delegate once to the activity's normal fallback.
                        isEnabled = false
                        try {
                            parent.onBackPressedDispatcher.onBackPressed()
                        } finally {
                            isEnabled = true
                        }
                    }
                }
            }
            parent.onBackPressedDispatcher.addCallback(parent, callback)
            onDispose { callback.remove() }
        }
    }
    CompositionLocalProvider(
        LocalOnBackPressedDispatcherOwner provides if (predictive) parent else commitOnly,
        content = content,
    )
}
