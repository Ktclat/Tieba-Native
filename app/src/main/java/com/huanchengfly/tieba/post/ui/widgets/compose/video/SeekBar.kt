package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.getDurationString
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBar(
    progress: Long,
    max: Long,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    secondaryProgress: Long? = null,
    onSeek: (progress: Long) -> Unit = {},
    onSeekStarted: (startedProgress: Long) -> Unit = {},
    onSeekStopped: (stoppedProgress: Long) -> Unit = {},
    seekerPopup: @Composable () -> Unit = {},
    showSeekerDuration: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = Color.White.copy(alpha = 0.6f),
) {
    if (max <= 0L) return
    var scrubbing by remember(max, enabled) { mutableStateOf<Float?>(null) }
    val position = scrubbing ?: (progress.coerceIn(0L, max).toFloat() / max)
    fun timeAt(fraction: Float) = (fraction.toDouble() * max).roundToLong().coerceIn(0L, max)
    val buffered = secondaryProgress?.let { it.coerceIn(0L, max).toFloat() / max } ?: 0f

    Column(modifier) {
        scrubbing?.let { fraction ->
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val width = constraints.maxWidth
                var previewWidth by remember { mutableStateOf(0) }
                Column(
                    modifier = Modifier.offset {
                        val x = (width * fraction).roundToInt() - previewWidth / 2
                        IntOffset(x.coerceIn(0, (width - previewWidth).coerceAtLeast(0)), 0)
                    }.onSizeChanged { previewWidth = it.width },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    seekerPopup()
                    if (showSeekerDuration) Text(
                        getDurationString(timeAt(fraction), false),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Box(contentAlignment = Alignment.Center) {
            LinearProgressIndicator(
                progress = { buffered },
                modifier = Modifier.fillMaxWidth().height(4.dp).clearAndSetSemantics { },
                color = secondaryColor,
                trackColor = Color.White.copy(alpha = 0.24f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            if (enabled) {
                val colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = Color.Transparent)
                Slider(
                    value = position,
                    onValueChange = { fraction ->
                        val time = timeAt(fraction)
                        if (scrubbing == null) onSeekStarted(time)
                        scrubbing = fraction
                        onSeek(time)
                    },
                    onValueChangeFinished = {
                        scrubbing?.let { onSeekStopped(timeAt(it)) }
                        scrubbing = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = colors,
                    track = { state ->
                        SliderDefaults.Track(sliderState = state, modifier = Modifier.height(4.dp), colors = colors)
                    },
                )
            } else {
                LinearProgressIndicator(
                    progress = { position },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = color,
                    trackColor = Color.Transparent,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
    }
}
