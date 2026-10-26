package com.huanchengfly.tieba.post.ui.page.settings.font

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import kotlin.math.roundToInt

/** Stateless editor: preview changes never write preferences; only Save invokes persistence. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FontSizeScreen(
    savedIndex: Int,
    draftIndex: Int,
    onDraftChange: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    saving: Boolean = false,
    saveError: String? = null,
) {
    val index = draftIndex.coerceIn(0, FontScaleSteps.LAST_INDEX)
    val colors = MaterialTheme.colorScheme
    val percentage = stringResource(R.string.native_font_percentage, FontScaleSteps.percentForIndex(index))
    val sliderLabel = stringResource(R.string.native_font_slider_label)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_custom_font_size)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !saving,
                        modifier = Modifier.semantics { testTag = "font-back" }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.button_back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = colors.surfaceContainerLow, tonalElevation = 0.dp) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onSave,
                        enabled = !saving && index != savedIndex,
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .semantics { testTag = "font-save" },
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 24.dp),
                    ) {
                        Text(stringResource(if (saving) R.string.native_font_saving else R.string.native_font_save))
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().semantics { testTag = "font-content" },
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "introduction") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.native_font_heading), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.native_font_help), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
                item(key = "preview") {
                    Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainerLow) {
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text(stringResource(R.string.native_font_preview), style = MaterialTheme.typography.labelLarge, color = colors.primary)
                            val density = LocalDensity.current
                            // Absolute draft scale, never multiplied by the saved scale a second time.
                            CompositionLocalProvider(
                                LocalDensity provides Density(density.density, FontScaleSteps.scaleForIndex(index))
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(stringResource(R.string.native_font_sample_title), style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.semantics { testTag = "font-preview-title" })
                                    Text(stringResource(R.string.native_font_sample_body), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.native_font_sample_caption), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                item(key = "controls") {
                    Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainerLow) {
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(percentage, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.semantics { testTag = "font-percentage" })
                            Slider(
                                value = index.toFloat(),
                                onValueChange = { onDraftChange(it.roundToInt().coerceIn(0, FontScaleSteps.LAST_INDEX)) },
                                valueRange = 0f..FontScaleSteps.LAST_INDEX.toFloat(),
                                steps = FontScaleSteps.LAST_INDEX - 1,
                                enabled = !saving,
                                modifier = Modifier.fillMaxWidth().semantics {
                                    testTag = "font-slider"
                                    contentDescription = sliderLabel
                                    stateDescription = percentage
                                },
                            )
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FilledTonalIconButton(
                                    onClick = { onDraftChange(index - 1) }, enabled = !saving && index > 0,
                                    modifier = Modifier.semantics { testTag = "font-decrease" },
                                ) { Icon(Icons.Rounded.Remove, stringResource(R.string.native_font_decrease)) }
                                OutlinedButton(
                                    onClick = { onDraftChange(FontScaleSteps.DEFAULT_INDEX) },
                                    enabled = !saving && index != FontScaleSteps.DEFAULT_INDEX,
                                    modifier = Modifier.weight(1f).semantics { testTag = "font-reset" },
                                ) { Text(stringResource(R.string.native_font_reset)) }
                                FilledTonalIconButton(
                                    onClick = { onDraftChange(index + 1) }, enabled = !saving && index < FontScaleSteps.LAST_INDEX,
                                    modifier = Modifier.semantics { testTag = "font-increase" },
                                ) { Icon(Icons.Rounded.Add, stringResource(R.string.native_font_increase)) }
                            }
                        }
                    }
                }
                if (saveError != null) {
                    item(key = "save-error") {
                        Text(saveError, color = colors.error, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.semantics { testTag = "font-save-error" })
                    }
                }
                item(key = "hint") {
                    Text(stringResource(R.string.native_font_apply_hint), color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
