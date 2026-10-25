package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
fun GroupHeader(
    title: String,
    color: Color = ExtendedTheme.colors.primary
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 10.dp, bottom = 5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
