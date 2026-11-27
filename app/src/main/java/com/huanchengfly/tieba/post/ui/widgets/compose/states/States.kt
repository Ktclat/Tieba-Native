@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.widgets.compose.states

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.StateIllustration
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen

val DefaultLoadingScreen: @Composable StateScreenScope.() -> Unit = {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}

val DefaultEmptyScreen: @Composable StateScreenScope.() -> Unit = {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.title_empty)) },
        image = { StateIllustration(ImageVector.vectorResource(R.drawable.ms_inbox)) },
        actions = {
            if (canReload) {
                Button(onClick = { reload() }, shapes = ButtonDefaults.shapes()) {
                    Text(text = stringResource(id = R.string.btn_refresh))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

val DefaultErrorScreen: @Composable StateScreenScope.() -> Unit = {
    Text(
        text = stringResource(id = R.string.error_tip),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun StateScreen(
    isEmpty: Boolean,
    isError: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onReload: (() -> Unit)? = null,
    clickToReload: Boolean = false,
    emptyScreen: @Composable StateScreenScope.() -> Unit = DefaultEmptyScreen,
    errorScreen: @Composable StateScreenScope.() -> Unit = DefaultErrorScreen,
    loadingScreen: @Composable StateScreenScope.() -> Unit = DefaultLoadingScreen,
    content: @Composable StateScreenScope.() -> Unit,
) {
    val stateScreenScope = remember(key1 = onReload) { StateScreenScope(onReload) }
    val clickableModifier = if (onReload != null && clickToReload) Modifier.clickable(
        enabled = isEmpty && !isLoading,
        onClick = onReload
    ) else Modifier
    Box(
        modifier = modifier
                then clickableModifier,
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            stateScreenScope.content()
        } else {
            if (isLoading) {
                stateScreenScope.loadingScreen()
            } else if (isError) {
                stateScreenScope.errorScreen()
            } else {
                stateScreenScope.emptyScreen()
            }
        }
    }
}

class StateScreenScope(
    private val onReload: (() -> Unit)? = null
) {
    val canReload: Boolean
        get() = onReload != null

    fun reload() {
        onReload?.invoke()
    }
}