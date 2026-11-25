package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.app.Application
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class VideoPlayerAllocationTest {
    @Test fun coverPlaybackAndSeekPreviewAllocateOnlyTheEnginesTheyUse() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val scope = MainScope()
        val engines = mutableListOf<ExoPlayer>()
        val controller = DefaultVideoPlayerController(context, VideoPlayerState(), scope, buildPlayer = {
            ExoPlayer.Builder(context).build().also(engines::add)
        })
        try {
            controller.initialize()
            controller.pause()
            controller.reset()
            controller.release()
            assertTrue(engines.isEmpty())

            controller.initialize()
            controller.play()
            assertEquals(1, engines.size)
            assertTrue(engines.first().playWhenReady)
            val surface = PlayerView(context)
            controller.playerViewAvailable(surface)
            controller.pause()
            assertFalse(engines.first().playWhenReady)
            assertSame(engines.first(), surface.player)

            val preview = PlayerView(context)
            controller.previewPlayerViewAvailable(preview)
            controller.previewPlayerViewAvailable(preview)
            assertEquals(2, engines.size)
            assertSame(engines.last(), preview.player)
            assertNotSame(surface.player, preview.player)
            controller.release()
            controller.release()
            assertEquals(2, engines.size)
        } finally {
            controller.release()
            scope.cancel()
        }
    }
}
