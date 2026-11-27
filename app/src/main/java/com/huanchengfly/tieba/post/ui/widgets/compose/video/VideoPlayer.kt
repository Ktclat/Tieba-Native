@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.compose.AsyncImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.getDurationString

internal val LocalVideoPlayerController =
    compositionLocalOf<DefaultVideoPlayerController> { error("VideoPlayerController is not initialized") }

@Composable
fun rememberVideoPlayerController(
    source: VideoPlayerSource? = null,
    thumbnailUrl: String? = null,
    fullScreenModeChangedListener: OnFullScreenModeChangedListener? = null,
    playWhenReady: Boolean = false,
): VideoPlayerController {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return rememberSaveable(
        context, coroutineScope,
        saver = object : Saver<DefaultVideoPlayerController, VideoPlayerState> {
            override fun restore(value: VideoPlayerState): DefaultVideoPlayerController {
                return DefaultVideoPlayerController(
                    context = context,
                    initialState = value.copy(isPlaying = playWhenReady),
                    coroutineScope = coroutineScope,
                    fullScreenModeChangedListener = fullScreenModeChangedListener
                ).apply {
                    source?.let { setSource(it) }
                }
            }

            override fun SaverScope.save(value: DefaultVideoPlayerController): VideoPlayerState {
                return value.currentState { it }
            }
        },
        init = {
            DefaultVideoPlayerController(
                context = context,
                initialState = VideoPlayerState(
                    thumbnailUrl = thumbnailUrl,
                    isPlaying = playWhenReady
                ),
                coroutineScope = coroutineScope,
                fullScreenModeChangedListener = fullScreenModeChangedListener
            ).apply {
                source?.let { setSource(it) }
            }
        }
    )
}

@Composable
fun VideoPlayer(
    videoPlayerController: VideoPlayerController,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = true,
    gesturesEnabled: Boolean = true,
    backgroundColor: Color = Color.Black
) {
    require(videoPlayerController is DefaultVideoPlayerController) {
        "Use [rememberVideoPlayerController] to create an instance of [VideoPlayerController]"
    }

    SideEffect {
        videoPlayerController.videoPlayerBackgroundColor = backgroundColor.value.toInt()
        videoPlayerController.enableControls(controlsEnabled)
        videoPlayerController.enableGestures(gesturesEnabled)
    }

    DisposableEffect(Unit) {
        videoPlayerController.initialize()
        onDispose {
            videoPlayerController.release()
        }
    }

    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalVideoPlayerController provides videoPlayerController
    ) {
        val startedPlay by videoPlayerController.collect { startedPlay || playbackState != PlaybackState.IDLE }
        val aspectRatio by videoPlayerController.collect { videoSize.first / videoSize.second }
        val supportFullScreen =
            remember(videoPlayerController) { videoPlayerController.supportFullScreen() }

        if (supportFullScreen) {
            val isFullScreen by videoPlayerController.collect { isFullScreen }

            BackHandler(enabled = isFullScreen) {
                Log.i("VideoPlayer", "handleBackPress")
                videoPlayerController.toggleFullScreen()
            }
        }

        Box(
            modifier = Modifier
                .background(color = backgroundColor)
                .fillMaxSize()
                .then(modifier)
        ) {
            if (startedPlay) {
                PlayerSurface(
                    modifier = Modifier
                        .aspectRatio(aspectRatio.takeUnless { it.isNaN() || it == 0f } ?: 2f)
                        .align(Alignment.Center)
                ) {
                    videoPlayerController.playerViewAvailable(it)
                }

                MediaController()
            } else {
                val thumbnailUrl by videoPlayerController.collect { thumbnailUrl }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailUrl != null) {
                        AsyncImage(
                            imageUri = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { videoPlayerController.play() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ms_play_arrow),
                            contentDescription = stringResource(id = R.string.btn_play),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.MediaController() {
    val videoPlayerController = LocalVideoPlayerController.current

    MediaControlGestures(modifier = Modifier.matchParentSize())
    MediaControlButtons(
        modifier = Modifier.matchParentSize()
    )

    val controlsVisible by videoPlayerController.collect { controlsVisible }

    if (controlsVisible) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .align(Alignment.BottomCenter),
        ) {
            ProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                PositionAndDuration()
                Spacer(modifier = Modifier.weight(1f))
                if (videoPlayerController.supportFullScreen()) {
                    FullScreenButton()
                }
            }
        }
    }
    if (!controlsVisible) {
        ProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter))
    }

}

@Composable
fun PositionAndDuration(
    modifier: Modifier = Modifier
) {
    val controller = LocalVideoPlayerController.current

    val positionText by controller.collect {
        getDurationString(currentPosition, false)
    }
    val durationText by controller.collect {
        getDurationString(duration, false)
    }

    Text(
        "$positionText/$durationText",
        style = TextStyle(
            shadow = Shadow(
                blurRadius = 8f,
                offset = Offset(2f, 2f)
            )
        ),
        modifier = modifier
    )
}

@Composable
private fun FullScreenButton() {
    val videoPlayerController = LocalVideoPlayerController.current
    val isFullScreen by videoPlayerController.collect { isFullScreen }
    val icon = if (isFullScreen) {
        ImageVector.vectorResource(R.drawable.ms_fullscreen_exit)
    } else {
        ImageVector.vectorResource(R.drawable.ms_fullscreen)
    }
    IconButton(
        onClick = videoPlayerController::toggleFullScreen,
        shapes = IconButtonDefaults.shapes(),
    ) {
        Icon(imageVector = icon, contentDescription = stringResource(id = R.string.btn_full_screen))
    }
}
