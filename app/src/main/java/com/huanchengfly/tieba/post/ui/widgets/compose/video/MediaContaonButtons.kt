@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.widgets.compose.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R

@Composable
fun MediaControlButtons(modifier: Modifier = Modifier) {
    val controller = LocalVideoPlayerController.current
    val visible by controller.collect { controlsEnabled && controlsVisible }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = controller::hideControls,
                )
            )
            PlayPauseButton()
        }
    }
}

@Composable
fun PlayPauseButton(modifier: Modifier = Modifier) {
    val controller = LocalVideoPlayerController.current
    val isPlaying by controller.collect { isPlaying }
    val playbackState by controller.collect { playbackState }
    FilledTonalIconButton(
        onClick = controller::togglePlaying,
        shapes = IconButtonDefaults.shapes(),
        modifier = modifier,
    ) {
        when {
            isPlaying -> Icon(Icons.Rounded.Pause, stringResource(R.string.btn_pause_video))
            playbackState == PlaybackState.BUFFERING -> LoadingIndicator(Modifier.size(24.dp))
            playbackState == PlaybackState.ENDED -> Icon(Icons.Rounded.Replay, stringResource(R.string.btn_replay_video))
            else -> Icon(Icons.Rounded.PlayArrow, stringResource(R.string.btn_play))
        }
    }
}
