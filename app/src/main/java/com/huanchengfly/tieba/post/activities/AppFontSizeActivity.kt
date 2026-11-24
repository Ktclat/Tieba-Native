package com.huanchengfly.tieba.post.activities

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.page.settings.font.FontScaleSteps
import com.huanchengfly.tieba.post.ui.page.settings.font.FontSizeScreen
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Native M3 editor. Drafts survive rotation, save atomically and never restart the app. */
class AppFontSizeActivity : BaseComposeActivity() {
    private var storedScale by mutableStateOf<Float?>(null)
    private var loadFailed by mutableStateOf(false)
    private var saving by mutableStateOf(false)
    private var saveError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadScale()
    }

    private fun loadScale() {
        loadFailed = false
        lifecycleScope.launch {
            try {
                storedScale = FontScaleSteps.normalized(dataStore.data.first()[floatPreferencesKey("fontScale")] ?: DEFAULT_FONT_SCALE)
            } catch (_: IOException) {
                loadFailed = true
            }
        }
    }

    @Composable
    override fun Content() {
        val initial = storedScale
        if (initial == null) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    if (loadFailed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.native_font_load_failed))
                            Button(onClick = ::loadScale) { Text(stringResource(R.string.native_font_retry)) }
                            Button(onClick = { finish() }) { Text(stringResource(R.string.button_back)) }
                        }
                    } else CircularProgressIndicator()
                }
            }
            return
        }
        val savedIndex = FontScaleSteps.indexForScale(initial)
        var draft by rememberSaveable(initial) { mutableIntStateOf(savedIndex) }
        // A write is awaited before leaving. A cancelled draft never touches the DataStore.
        BackHandler(enabled = saving) { }
        FontSizeScreen(
            savedIndex = savedIndex,
            draftIndex = draft,
            onDraftChange = { draft = it },
            onSave = { saveScale(draft) },
            onBack = { finish() },
            saving = saving,
            saveError = saveError,
        )
    }

    private fun saveScale(index: Int) {
        if (saving) return
        saving = true
        saveError = null
        lifecycleScope.launch {
            try {
                dataStore.edit { it[floatPreferencesKey("fontScale")] = FontScaleSteps.scaleForIndex(index) }
                setResult(RESULT_OK)
                finish()
            } catch (_: IOException) {
                saveError = getString(R.string.native_font_save_failed)
            } finally {
                saving = false
            }
        }
    }

    companion object {
        // Keep upstream constants/API for callers and existing preference values.
        const val FONT_SCALE_MIN = 0.8f
        const val FONT_SCALE_MAX = 1.3f
        const val FONT_SCALE_STEP = 0.05f
        const val DEFAULT_FONT_SCALE = 1f
        val SIZE_TEXT_MAPPING = mapOf(
            R.string.text_size_small to 0..1,
            R.string.text_size_little_small to 2..3,
            R.string.text_size_default to 4..4,
            R.string.text_size_little_large to 5..6,
            R.string.text_size_large to 7..8,
            R.string.text_size_very_large to 9..10,
        )
    }
}
