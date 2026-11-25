package com.huanchengfly.tieba.post.ui.widgets.compose.picker

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.drop
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val clock24 = DateTimeFormatter.ofPattern("H:mm", Locale.US)
private val clock12 = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val output24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val output12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(
    currentTime: String,
    onTimeChanged: (String) -> Unit,
    is24TimeFormat: Boolean,
    modifier: Modifier = Modifier,
) {
    val initial = remember(currentTime) {
        val input = currentTime.trim().uppercase(Locale.US)
        val format = if (input.endsWith("AM") || input.endsWith("PM")) clock12 else clock24
        runCatching { LocalTime.parse(input, format) }.getOrDefault(LocalTime.MIDNIGHT)
    }
    val state = rememberTimePickerState(initial.hour, initial.minute, is24TimeFormat)
    val notify by rememberUpdatedState(onTimeChanged)
    LaunchedEffect(state, is24TimeFormat) {
        snapshotFlow { LocalTime.of(state.hour, state.minute) }.drop(1).collect { time ->
            notify(time.format(if (is24TimeFormat) output24 else output12))
        }
    }
    TimeInput(state = state, modifier = modifier)
}
