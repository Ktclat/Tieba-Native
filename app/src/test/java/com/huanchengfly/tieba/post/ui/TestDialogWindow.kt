@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi::class)

package com.huanchengfly.tieba.post.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialogOverride
import androidx.compose.material3.BasicAlertDialogOverrideScope
import androidx.compose.material3.LocalBasicAlertDialogOverride
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun InTestDialogWindow(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBasicAlertDialogOverride provides TestDialogWindow, content = content)
}

// The host JVM has one drawable window. Render the production AlertDialog content in that window.
private object TestDialogWindow : BasicAlertDialogOverride {
    @Composable
    override fun BasicAlertDialogOverrideScope.BasicAlertDialog() {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier.widthIn(min = 280.dp, max = 320.dp), propagateMinConstraints = true) { content() }
        }
    }
}
