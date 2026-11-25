package com.huanchengfly.tieba.post.ui.page

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalAppDarkTheme
import com.ramcosta.composedestinations.annotation.Destination
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

@Destination
@Composable
fun MonetTestPage() {
    val context = LocalContext.current
    val dark = LocalAppDarkTheme.current
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else MaterialTheme.colorScheme
    val swatches = remember(scheme) {
        ColorScheme::class.declaredMemberProperties
            .filter { it.visibility == KVisibility.PUBLIC && it.returnType.classifier == Color::class }
            .sortedBy { it.name }
            .map { it.name to (it.get(scheme) as Color) }
    }
    LazyColumn(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
        items(swatches, key = { it.first }) { (name, color) ->
            ListItem(
                headlineContent = { Text(name) },
                leadingContent = {
                    Surface(shape = MaterialTheme.shapes.small, color = color) {
                        Box(Modifier.size(40.dp))
                    }
                },
            )
        }
    }
}
