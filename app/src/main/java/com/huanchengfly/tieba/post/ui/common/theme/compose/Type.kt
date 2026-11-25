package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.material.Typography
import androidx.compose.material3.Typography as Material3Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val DefaultTextStyle = TextStyle.Default.copy(fontFamily = FontFamily.SansSerif)

val ExpressiveTypography = Material3Typography(
    displayLarge = DefaultTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 56.sp),
    headlineLarge = DefaultTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = DefaultTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = DefaultTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = DefaultTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleMedium = DefaultTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = DefaultTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp),
    bodyLarge = DefaultTextStyle.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = DefaultTextStyle.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = DefaultTextStyle.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = DefaultTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = DefaultTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = DefaultTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

/** Older text slots use the same type scale as the application's Material 3 theme. */
val Typography = with(ExpressiveTypography) {
    Typography(
        h1 = displayLarge,
        h2 = displayMedium,
        h3 = displaySmall,
        h4 = headlineLarge,
        h5 = headlineSmall,
        h6 = titleLarge,
        subtitle1 = titleMedium,
        subtitle2 = titleSmall,
        body1 = bodyLarge,
        body2 = bodyMedium,
        caption = bodySmall,
        button = labelLarge,
        overline = labelSmall,
    )
}
