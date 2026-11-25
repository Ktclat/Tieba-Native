package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun BaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
    )
}

@Composable
fun CounterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = Int.MAX_VALUE,
    countWhitespace: Boolean = true,
    onLengthBeyondRestrict: ((String) -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    fun counted(c: Char) = countWhitespace || !c.isWhitespace()
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            var count = 0
            val overflowAt = next.indexOfFirst { counted(it) && ++count > maxLength }
            if (overflowAt < 0) onValueChange(next) else {
                onValueChange(next.take(overflowAt))
                onLengthBeyondRestrict?.invoke(next)
            }
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        supportingText = {
            val count = value.count(::counted)
            Text(if (maxLength == Int.MAX_VALUE) count.toString() else "$count/$maxLength")
        },
    )
}

@Composable
fun ProvideContentColor(color: Color, alpha: Float = color.alpha, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides color.copy(alpha = alpha), content = content)
}
