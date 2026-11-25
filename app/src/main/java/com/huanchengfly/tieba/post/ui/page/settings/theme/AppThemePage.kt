package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.fetch.newResourceUri
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.util.Locale
import kotlin.math.roundToInt

@Destination
@Composable
fun AppThemePage(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val themeValues = stringArrayResource(R.array.theme_values)
    val themeNames = stringArrayResource(R.array.themeNames)
    val currentTheme by ThemeUtil.themeState
    val dynamic by context.dataStore.collectPreferenceAsState(booleanPreferencesKey("useDynamicColorTheme"), false)
    val backgroundPath by context.dataStore.collectPreferenceAsState(stringPreferencesKey("translucent_theme_background_path"), "")
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var customColor by remember {
        mutableIntStateOf(App.ThemeDelegate.getColorByAttr(context, R.attr.colorPrimary, ThemeUtil.THEME_CUSTOM))
    }

    if (showEditor) {
        CustomColorDialog(
            initialColor = customColor,
            initialToolbar = context.appPreferences.toolbarPrimaryColor,
            initialDarkIcons = context.appPreferences.customStatusBarFontDark,
            dismiss = { showEditor = false },
            save = { argb, toolbar, darkIcons ->
                customColor = argb
                context.appPreferences.customPrimaryColor = String.format(Locale.ROOT, "#%08x", argb)
                context.appPreferences.toolbarPrimaryColor = toolbar
                context.appPreferences.customStatusBarFontDark = darkIcons || !toolbar
                ThemeUtil.setUseDynamicTheme(false)
                ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
                showEditor = false
            },
        )
    }

    MyScaffold(topBar = {
        TitleCentredToolbar(
            title = { Text(stringResource(R.string.title_theme)) },
            navigationIcon = { BackNavigationIcon(onBackPressed = { navigator.navigateUp() }) },
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding).selectableGroup()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    val scheme = dynamicLightColorScheme(context)
                    ThemeOption(
                        name = stringResource(R.string.title_dynamic_theme),
                        selected = dynamic,
                        onClick = { ThemeUtil.setUseDynamicTheme(true) },
                        swatch = { ColorSwatch(scheme.primary, scheme.tertiary) },
                    )
                }
            }
            item {
                ThemeOption(
                    name = stringResource(R.string.title_custom_color),
                    selected = !dynamic && currentTheme == ThemeUtil.THEME_CUSTOM,
                    onClick = { showEditor = true },
                    swatch = { ColorSwatch(Color(customColor)) },
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
            itemsIndexed(themeValues.toList(), key = { _, value -> value }) { index, theme ->
                val tint = remember(theme, context) {
                    val attr = if (ThemeUtil.isNightMode(theme)) R.attr.colorBackground else R.attr.colorNewPrimary
                    Color(App.ThemeDelegate.getColorByAttr(context, attr, theme))
                }
                ThemeOption(
                    name = themeNames[index],
                    selected = !dynamic && currentTheme == theme,
                    onClick = {
                        ThemeUtil.switchTheme(theme)
                        ThemeUtil.setUseDynamicTheme(false)
                    },
                    swatch = { ColorSwatch(tint) },
                )
            }
        }
    }
}

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
        modifier = if (opensEditor) Modifier.clickable(onClick = onClick)
                   else Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        headlineContent = { Text(name) },
        leadingContent = swatch,
        trailingContent = {
            if (opensEditor && !selected) Icon(Icons.Rounded.Edit, contentDescription = null)
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
            TextButton(onClick = { save(color.toArgb(), toolbar, darkIcons) }, enabled = validHex) {
                Text(stringResource(R.string.button_finish))
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.button_cancel)) } },
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
