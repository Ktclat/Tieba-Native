package com.huanchengfly.tieba.post.utils.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalAppThemeName
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalDisplayPreferences
import com.huanchengfly.tieba.post.utils.ColorUtils
import com.huanchengfly.tieba.post.utils.ThemeUtil

fun Color.darken(i: Float = 0.1F): Color {
    return Color(ColorUtils.getDarkerColor(toArgb(), i))
}

@Composable
fun Color.calcStatusBarColor(): Color {
    val prefs = LocalDisplayPreferences.current
    val darker = prefs.toolbarPrimaryColor && prefs.darkerStatusBar && !prefs.dynamicColor &&
        !ThemeUtil.isTranslucentTheme(LocalAppThemeName.current)
    return if (darker) darken() else this
}
