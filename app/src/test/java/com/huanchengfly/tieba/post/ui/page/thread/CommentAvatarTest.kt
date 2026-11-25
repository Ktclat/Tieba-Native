package com.huanchengfly.tieba.post.ui.page.thread

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.github.panpf.sketch.Sketch
import com.huanchengfly.tieba.post.utils.StringUtil
import com.github.panpf.sketch.SketchFactory
import com.github.panpf.sketch.SketchSingleton
import com.github.panpf.sketch.http.OkHttpStack
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.stateimage.StateImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CommentAvatarTest {
    @Test fun monogramKeepsTheWholeFirstGrapheme() {
        assertEquals("李", commentAvatarMonogram("  李明"))
        assertEquals("A\u0301", commentAvatarMonogram("A\u0301lex"))
        assertEquals("👩🏽‍💻", commentAvatarMonogram("👩🏽‍💻开发者"))
        assertEquals("🇨🇳", commentAvatarMonogram("🇨🇳用户"))
        assertEquals("", commentAvatarMonogram("  "))
    }

    @Test fun portraitPreservesSuppliedUrlsAndResolvesTokensOverHttps() {
        assertNull(commentPortraitUrl(" "))
        val canonical = "https://himg.bdimg.com/sys/portrait/item/tb.token?t=1729"
        assertEquals(canonical, commentPortraitUrl(" tb.token?t=1729 "))
        assertEquals(canonical, commentPortraitUrl("//tb.himg.baidu.com/sys/portrait/item/tb.token?t=1729"))
        assertEquals(canonical, StringUtil.getAvatarUrl("http://tb.himg.baidu.com/sys/portrait/item/tb.token?t=1729"))
        assertEquals(canonical, StringUtil.getAvatarUrl("/sys/portrait/item/tb.token?t=1729"))
        assertEquals("https://himg.bdimg.com/sys/portraith/item/tb.token?t=1729",
            StringUtil.getBigAvatarUrl("tb.token?t=1729"))
        assertEquals("http://example.org/p?a=1", commentPortraitUrl("http://example.org/p?a=1"))
        assertEquals("https://example.org/p", commentPortraitUrl("HTTPS://example.org/p"))
        assertEquals("https://example.org/p", commentPortraitUrl("//example.org/p"))
        assertEquals("https://example.org/p", commentPortraitUrl("https://example.org/p"))
    }

    @Test fun authorIdentityKeepsItsPaletteAcrossNamesAndPortraitChanges() {
        val first = commentAvatarPaletteIndex("旧昵称", "https://example.org/p", 19)
        assertEquals(first, commentAvatarPaletteIndex("新昵称", null, 19))
        assertEquals(
            commentAvatarPaletteIndex("作者", "https://example.org/p", null),
            commentAvatarPaletteIndex("新昵称", "https://example.org/p", 0),
        )
        assertEquals(commentAvatarPaletteIndex("作者", null, null), commentAvatarPaletteIndex(" 作者 ", null, null))
        assertTrue(first in 0..2)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = AvatarFixtureApplication::class, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CommentAvatarRenderingTest {
    @get:Rule val compose = createComposeRule()

    @After fun releaseFixture() = SketchSingleton.reset()

    @Test fun showsMonogramUntilDecodedAndRestoresItAfterAnError() {
        val fixture = ApplicationProvider.getApplicationContext<AvatarFixtureApplication>()
        SketchSingleton.setSketch(fixture)
        var portrait by mutableStateOf<String?>(null)
        lateinit var view: View
        compose.setContent {
            val currentView = LocalView.current
            SideEffect { view = currentView }
            MaterialTheme {
                Box(Modifier.testTag("avatar")) {
                    CommentAvatar("Mina", portrait, authorId = 19)
                }
            }
        }

        val bounds = compose.onNodeWithTag("avatar").fetchSemanticsNode().boundsInRoot
        fun pixels(): IntArray = compose.runOnIdle {
            // Sample the inscribed square so native circle-edge antialiasing cannot
            // change the comparison. This includes both the glyph and the container.
            val inset = (bounds.width * 0.17f).roundToInt()
            val width = bounds.width.roundToInt() - 2 * inset
            val height = bounds.height.roundToInt() - 2 * inset
            val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(image)
                canvas.translate(-bounds.left - inset, -bounds.top - inset)
                view.draw(canvas)
                IntArray(width * height).also { image.getPixels(it, 0, width, 0, 0, width, height) }
            } finally {
                image.recycle()
            }
        }

        val monogram = pixels()
        assertTrue("Monogram must contain ink over the container", monogram.distinct().size > 4)
        fun awaitSketch(condition: () -> Boolean) {
            compose.waitUntil(10_000) {
                compose.waitForIdle()
                shadowOf(Looper.getMainLooper()).idle()
                condition()
            }
        }
        compose.runOnIdle { portrait = "tb.fixture?t=2" }
        try {
            awaitSketch {
                check(fixture.error == null) { "Unexpected Sketch error: ${fixture.error}" }
                fixture.started.count == 0L
            }
            assertArrayEquals("Pending request should retain the monogram", monogram, pixels())
        } finally {
            fixture.release.countDown()
        }
        awaitSketch {
            check(fixture.error == null) { "Unexpected decode error: ${fixture.error}" }
            pixels().count { it == Color.MAGENTA } > monogram.size / 2
        }

        compose.runOnIdle { portrait = "https://fixture.invalid/missing.png" }
        awaitSketch { fixture.failed.count == 0L }
        assertArrayEquals("Failed request should retain the monogram", monogram, pixels())
    }
}

/** Sketch still fetches and decodes real PNG bytes. The HTTP interceptor terminates every request locally. */
class AvatarFixtureApplication : Application(), SketchFactory {
    val started = CountDownLatch(1)
    val release = CountDownLatch(1)
    val failed = CountDownLatch(1)
    @Volatile var error: Throwable? = null

    override fun createSketch(): Sketch {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val ok = chain.request().url.toString() == "https://himg.bdimg.com/sys/portrait/item/tb.fixture?t=2"
            val bytes = if (ok) {
                started.countDown()
                check(release.await(30, TimeUnit.SECONDS)) { "PNG fixture was never released" }
                val image = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                try {
                    image.eraseColor(Color.MAGENTA)
                    ByteArrayOutputStream().use { output ->
                        check(image.compress(Bitmap.CompressFormat.PNG, 100, output))
                        output.toByteArray()
                    }
                } finally {
                    image.recycle()
                }
            } else byteArrayOf()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(if (ok) 200 else 404).message(if (ok) "OK" else "Not found")
                .body(bytes.toResponseBody("image/png".toMediaType())).build()
        }.build()
        val errorImage = object : StateImage {
            override fun getDrawable(sketch: Sketch, request: ImageRequest, throwable: Throwable?): Drawable {
                error = throwable
                failed.countDown()
                return ColorDrawable(Color.RED)
            }
        }
        return Sketch.Builder(this)
            .httpStack(OkHttpStack(client))
            .globalImageOptions(ImageOptions.Builder().placeholder(ColorDrawable(Color.GREEN)).error(errorImage).build())
            .build()
    }
}
