package com.huanchengfly.tieba.post.ui.widgets.compose

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class VoicePlaybackTest {
    @Test fun removingAnOldReplyCannotStopTheNewReplyEvenWhenTheirAudioUrlMatches() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val original = Any()
        val duplicate = Any()
        // One millisecond of silent PCM, playable without a file or network connection.
        val url = "data:audio/wav;base64,UklGRiwAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQgAAACAgICAgICAgA=="
        assertNull(VoicePlayback.owner)
        try {
            VoicePlayback.toggle(context, original, url)
            VoicePlayback.toggle(context, duplicate, url)
            VoicePlayback.release(original)
            assertSame(duplicate, VoicePlayback.owner)
            VoicePlayback.release(duplicate)
            assertNull(VoicePlayback.owner)
        } finally {
            VoicePlayback.release(original)
            VoicePlayback.release(duplicate)
        }
    }
}
