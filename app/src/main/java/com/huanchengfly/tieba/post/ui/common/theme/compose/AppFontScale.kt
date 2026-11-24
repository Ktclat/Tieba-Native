package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.page.settings.font.FontScaleSteps
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Apply the saved app scale once, at the Compose root; do not multiply a system scale twice. */
@Composable
fun ProvideAppFontScale(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val density = LocalDensity.current
    val scaleFlow = remember(context) {
        context.dataStore.data
            .map { FontScaleSteps.normalized(it[floatPreferencesKey("fontScale")] ?: 1f) }
            .distinctUntilChanged()
            .catch { error ->
                if (error is IOException) emit(1f) else throw error
            }
    }
    val fontScale by scaleFlow.collectAsStateWithLifecycle(
        initialValue = FontScaleSteps.normalized(density.fontScale),
    )
    val appDensity = remember(density.density, fontScale) {
        Density(density = density.density, fontScale = fontScale)
    }
    CompositionLocalProvider(LocalDensity provides appDensity, content = content)
}
