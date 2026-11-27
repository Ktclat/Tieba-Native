@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.fetch.newResourceUri
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalAppDarkTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalDisplayPreferences
import com.huanchengfly.tieba.post.ui.common.theme.compose.appColorScheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Destination
@Composable
fun AppThemePage(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val currentTheme by ThemeUtil.themeState
    val prefs = LocalDisplayPreferences.current
    val dark = LocalAppDarkTheme.current
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && prefs.dynamicColor
    val storedPalette = ThemeUtil.paletteForTheme(currentTheme, prefs.accentPalette, prefs.previousDayPalette)
    val selectedPalette = when (storedPalette) {
        ThemeUtil.THEME_BLUE_DARK -> ThemeUtil.THEME_BLUE
        ThemeUtil.THEME_GREY_DARK, ThemeUtil.THEME_AMOLED_DARK -> ThemeUtil.THEME_DEFAULT
        else -> storedPalette
    }
    val backgroundPath by context.dataStore.collectPreferenceAsState(stringPreferencesKey(ThemeUtil.KEY_TRANSLUCENT_THEME_BACKGROUND_PATH), "")
    val scope = rememberCoroutineScope()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val customColor = prefs.customSeed ?: Color(context.getColor(R.color.tieba))
    val translucent = ThemeUtil.isTranslucentTheme(currentTheme)

    if (showEditor) {
        CustomColorDialog(
            initialColor = customColor.toArgb(),
            initialToolbar = prefs.toolbarPrimaryColor,
            initialDarkIcons = prefs.customDarkStatusIcons,
            dismiss = { showEditor = false },
            save = { argb, toolbar, darkIcons ->
                scope.launch {
                    ThemeUtil.selectPalette(ThemeUtil.THEME_CUSTOM, argb, toolbar, darkIcons)
                    showEditor = false
                }
            },
        )
    }

    MyScaffold(topBar = {
        TitleCentredToolbar(
            title = { Text(stringResource(R.string.title_theme)) },
            navigationIcon = { BackNavigationIcon(onBackPressed = { navigator.navigateUp() }) },
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    val colors = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    ListItem(
                        modifier = Modifier.toggleable(dynamic, role = Role.Switch) { enabled ->
                            scope.launch { ThemeUtil.setWallpaperColors(enabled) }
                        },
                        headlineContent = { Text(stringResource(R.string.theme_follow_wallpaper)) },
                        supportingContent = { Text(stringResource(R.string.theme_wallpaper_summary)) },
                        leadingContent = { ColorSwatch(colors.primary, colors.tertiary) },
                        trailingContent = { Switch(checked = dynamic, onCheckedChange = null) },
                    )
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.theme_app_palette), style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        modifier = Modifier.selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        paletteOptions.forEach { (palette, label) ->
                            val scheme = remember(context, palette, dark) {
                                appColorScheme(context, palette, dark, false, customSeed = null)
                            }
                            val selected = !dynamic && !translucent && selectedPalette == palette
                            FilterChip(
                                selected = selected,
                                onClick = { scope.launch { ThemeUtil.selectPalette(palette) } },
                                label = { Text(stringResource(label)) },
                                leadingIcon = {
                                    Box(Modifier.size(18.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
                                        if (selected) Icon(ImageVector.vectorResource(R.drawable.ms_check), null, Modifier.size(14.dp), tint = scheme.onPrimary)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            item {
                ThemeOption(
                    name = stringResource(R.string.theme_custom_color),
                    selected = !dynamic && !translucent && selectedPalette == ThemeUtil.THEME_CUSTOM,
                    onClick = { showEditor = true },
                    swatch = { ColorSwatch(customColor) },
                    opensEditor = true,
                )
            }
            item {
                ThemeOption(
                    name = stringResource(R.string.title_theme_translucent),
                    selected = !dynamic && ThemeUtil.isTranslucentTheme(currentTheme),
                    onClick = { context.goToActivity<TranslucentThemeActivity>() },
                    swatch = {
                        AsyncImage(
                            imageUri = if (backgroundPath.isBlank()) newResourceUri(R.drawable.user_header) else newFileUri(backgroundPath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                        )
                    },
                    opensEditor = true,
                )
            }
        }
    }
}

private val paletteOptions = listOf(
    ThemeUtil.THEME_DEFAULT to R.string.theme_expressive,
    ThemeUtil.THEME_BLUE to R.string.theme_tieba_blue,
    ThemeUtil.THEME_GREEN to R.string.theme_forest,
    ThemeUtil.THEME_AMBER to R.string.theme_amber,
    ThemeUtil.THEME_PINK to R.string.theme_pink,
    ThemeUtil.THEME_RED to R.string.theme_red,
    ThemeUtil.THEME_PURPLE to R.string.theme_purple,
    ThemeUtil.THEME_BLACK to R.string.theme_graphite,
)

@Composable
private fun ColorSwatch(color: Color, endColor: Color = color) {
    Box(
        Modifier.size(36.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(color, endColor)))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}

@Composable
private fun ThemeOption(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    swatch: @Composable () -> Unit,
    opensEditor: Boolean = false,
) {
    ListItem(
        modifier = Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        headlineContent = { Text(name) },
        leadingContent = swatch,
        trailingContent = {
            if (opensEditor && !selected) Icon(ImageVector.vectorResource(R.drawable.ms_edit), contentDescription = null)
            else RadioButton(selected = selected, onClick = null)
        },
    )
}

@Composable
internal fun CustomColorDialog(
    initialColor: Int,
    initialToolbar: Boolean,
    initialDarkIcons: Boolean,
    dismiss: () -> Unit,
    save: (Int, Boolean, Boolean) -> Unit,
) {
    val initial = remember(initialColor) { FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor, it) } }
    var hue by rememberSaveable { mutableFloatStateOf(initial[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(initial[1]) }
    var brightness by rememberSaveable { mutableFloatStateOf(initial[2]) }
    var hex by rememberSaveable { mutableStateOf(rgbHex(initialColor)) }
    var toolbar by rememberSaveable { mutableStateOf(initialToolbar) }
    var darkIcons by rememberSaveable { mutableStateOf(initialDarkIcons) }
    val color = Color.hsv(hue, saturation, brightness)
    val validHex = HexColor.matches(hex)
    fun updateChannels(h: Float, s: Float, v: Float) {
        hue = h
        saturation = s
        brightness = v
        hex = rgbHex(Color.hsv(h, s, v).toArgb())
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.title_custom_theme)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorSwatch(color)
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { input ->
                            hex = input
                            if (HexColor.matches(input)) {
                                val channels = FloatArray(3)
                                android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(input), channels)
                                hue = channels[0]
                                saturation = channels[1]
                                brightness = channels[2]
                            }
                        },
                        label = { Text(stringResource(R.string.theme_color_hex)) },
                        singleLine = true,
                        isError = !validHex,
                        modifier = Modifier.weight(1f),
                    )
                }
                ColorChannel(stringResource(R.string.theme_color_hue), hue, 360f, "${hue.roundToInt()}°") {
                    updateChannels(it, saturation, brightness)
                }
                ColorChannel(stringResource(R.string.theme_color_saturation), saturation, 1f, "${(saturation * 100).roundToInt()}%") {
                    updateChannels(hue, it, brightness)
                }
                ColorChannel(stringResource(R.string.theme_color_brightness), brightness, 1f, "${(brightness * 100).roundToInt()}%") {
                    updateChannels(hue, saturation, it)
                }
                ColorOption(stringResource(R.string.tip_toolbar_primary_color), toolbar) { toolbar = it }
                if (toolbar) ColorOption(stringResource(R.string.tip_status_bar_font), darkIcons) { darkIcons = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { save(color.toArgb(), toolbar, darkIcons) }, enabled = validHex, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.button_finish))
            }
        },
        dismissButton = { TextButton(onClick = dismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.button_cancel)) } },
    )
}

private val HexColor = Regex("#[0-9a-fA-F]{6}")

private fun rgbHex(color: Int) = String.format(Locale.ROOT, "#%06X", color and 0xFFFFFF)

@Composable
private fun ColorChannel(label: String, value: Float, max: Float, display: String, onValueChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(display, style = MaterialTheme.typography.labelLarge)
    }
    Slider(
        value = value, onValueChange = onValueChange, valueRange = 0f..max,
        modifier = Modifier.semantics { contentDescription = label },
    )
}

@Composable
private fun ColorOption(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label)
    }
}
