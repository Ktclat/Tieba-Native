package com.huanchengfly.tieba.post.ui.widgets.compose.picker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

private class DefaultPickerColors(
    private val itemColor: Color,
    private val disabledItemColor: Color,
    private val selectedItemColor: Color,
    private val disabledSelectedItemColor: Color
) : PickerColors {

    @Composable
    override fun itemColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(if (enabled) itemColor else disabledItemColor)

    @Composable
    override fun selectedItemColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(if (enabled) selectedItemColor else disabledSelectedItemColor)
}

@Stable
interface PickerColors {
    @Composable
    fun itemColor(enabled: Boolean): State<Color>

    @Composable
    fun selectedItemColor(enabled: Boolean): State<Color>
}

object PickerDefaults {
    @Composable
    fun pickerColors(
        itemColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledItemColor: Color = itemColor.copy(alpha = 0.38f),
        selectedItemColor: Color = MaterialTheme.colorScheme.primary,
        disabledSelectedItemColor: Color = disabledItemColor
    ): PickerColors =
        DefaultPickerColors(
            itemColor = itemColor,
            disabledItemColor = disabledItemColor,
            selectedItemColor = selectedItemColor,
            disabledSelectedItemColor = disabledSelectedItemColor
        )
}