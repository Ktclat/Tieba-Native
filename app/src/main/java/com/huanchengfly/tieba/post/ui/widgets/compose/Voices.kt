@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.widgets.compose

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.FileUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VoicePlayer(url: String, duration: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = remember(url) { Any() }
    val active = VoicePlayback.owner === owner
    val playing = active && VoicePlayback.playing
    val buffering = active && VoicePlayback.buffering
    var elapsed by remember(owner, active) { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        onDispose { VoicePlayback.release(owner) }
    }
    if (active) {
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { VoicePlayback.release(owner) }
        LaunchedEffect(owner, playing) {
            while (isActive) {
                val position = VoicePlayback.position
                elapsed = (position / 1_000).toInt()
                if (!playing) break
                delay(1_000L - position % 1_000L)
            }
        }
    }
    val tooltip = rememberTooltipState(isPersistent = true)
    TooltipBox(
        modifier = modifier,
        state = tooltip,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        focusable = true,
        hasAction = true,
        tooltip = {
            Surface(shape = MenuDefaults.shape, color = MenuDefaults.containerColor, shadowElevation = MenuDefaults.ShadowElevation) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_save_audio)) },
                    leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ms_download), contentDescription = null) },
                    onClick = {
                        tooltip.dismiss()
                        val name = Uri.parse(url).getQueryParameter("voice_md5") ?: System.currentTimeMillis().toString()
                        FileUtil.downloadBySystem(context, FileUtil.FILE_TYPE_AUDIO, url, "$name.mp3")
                    },
                )
            }
        },
    ) {
        FilledTonalButton(onClick = { VoicePlayback.toggle(context, owner, url) }, shapes = ButtonDefaults.shapes()) {
            when {
                buffering -> LoadingIndicator(Modifier.size(ButtonDefaults.IconSize))
                playing -> Icon(ImageVector.vectorResource(R.drawable.ms_pause), stringResource(R.string.pause_voice), Modifier.size(ButtonDefaults.IconSize))
                else -> Icon(ImageVector.vectorResource(R.drawable.ms_play_arrow), stringResource(R.string.play_voice), Modifier.size(ButtonDefaults.IconSize))
            }
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            val seconds = if (active) elapsed else duration.coerceAtLeast(0) / 1_000
            Text(if (seconds >= 60) "%d:%02d".format(seconds / 60, seconds % 60) else "$seconds″")
        }
    }
}

internal object VoicePlayback : Player.Listener {
    var owner by mutableStateOf<Any?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var buffering by mutableStateOf(false)
        private set
    private var player: ExoPlayer? = null
    val position get() = player?.currentPosition?.coerceAtLeast(0L) ?: 0L

    fun toggle(context: Context, newOwner: Any, url: String) {
        val current = player ?: ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build().also { it.addListener(this); player = it }
        if (owner !== newOwner) {
            owner = newOwner
            current.setMediaItem(MediaItem.fromUri(url))
            current.prepare()
            current.play()
        } else if (current.playWhenReady) {
            current.pause()
        } else {
            current.play()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }

    override fun onPlaybackStateChanged(playbackState: Int) {
        buffering = playbackState == Player.STATE_BUFFERING
        if (playbackState == Player.STATE_ENDED) owner?.let(::release)
    }

    override fun onPlayerError(error: PlaybackException) { owner?.let(::release) }

    fun release(expectedOwner: Any) {
        if (owner !== expectedOwner) return
        player?.removeListener(this)
        player?.release()
        player = null
        owner = null
        playing = false
        buffering = false
    }
}
