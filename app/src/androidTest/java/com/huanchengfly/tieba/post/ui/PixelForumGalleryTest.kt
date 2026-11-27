package com.huanchengfly.tieba.post.ui

import android.app.Instrumentation
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.calculateWindowSizeClass
import com.huanchengfly.tieba.post.ui.page.forum.ForumContent
import com.huanchengfly.tieba.post.ui.page.forum.ForumPartialChange
import com.huanchengfly.tieba.post.ui.page.forum.ForumUiState
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideShouldLoad
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelForumGalleryTest {
    @get:Rule(order = 0) val compose = createAndroidComposeRule<MainActivityV2>()
    @get:Rule(order = 1) val failureScreen = object : TestWatcher() {
        override fun failed(e: Throwable, description: Description) {
            runCatching { capture("unexpected-${description.methodName}") }
        }
    }
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val target get() = instrumentation.targetContext
    private val device get() = instrumentation.uiAutomation
    private val directory get() = File(target.filesDir, "phone-review").apply { mkdirs() }

    private fun installContent(content: @Composable () -> Unit) {
        val activity = compose.activity
        compose.runOnUiThread {
            activity.setContent {
                CompositionLocalProvider(LocalWindowSizeClass provides calculateWindowSizeClass(activity)) {
                    TiebaLiteTheme { content() }
                }
            }
        }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun tapText(text: String) {
        awaitText(text)
        compose.onNodeWithText(text).assertIsDisplayed().performTouchInput { click() }
    }

    private fun capture(name: String, photoColor: Int? = null) {
        val end = SystemClock.uptimeMillis() + 12_000
        do {
            val raw = checkNotNull(device.takeScreenshot())
            val frame = if (raw.config == Bitmap.Config.HARDWARE) {
                raw.copy(Bitmap.Config.ARGB_8888, false).also { raw.recycle() }
            } else raw
            try {
                // Check the large image itself, above the controls and away from screen edges.
                val hasPhoto = photoColor == null || (frame.height / 3 until 2 * frame.height / 3 step 24).sumOf { y ->
                    (frame.width / 8 until 7 * frame.width / 8 step 24).count { x ->
                        val c = frame.getPixel(x, y)
                        abs(Color.red(c) - Color.red(photoColor)) < 5 &&
                            abs(Color.green(c) - Color.green(photoColor)) < 5 &&
                            abs(Color.blue(c) - Color.blue(photoColor)) < 5
                    }
                } > 140
                if (hasPhoto) {
                    File(directory, "$name.png").outputStream().use { check(frame.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    return
                }
            } finally {
                frame.recycle()
            }
            SystemClock.sleep(120)
        } while (SystemClock.uptimeMillis() < end)
        error("Expected fixture image did not become visible on $name")
    }

    @Test fun openForumFromHistoryRetryReturnAndShowForumData() {
        val name = "徒步"
        val title = target.getString(R.string.title_forum, name)
        val history = target.getString(R.string.title_history)
        val forums = target.getString(R.string.title_history_forum)
        val retry = target.getString(R.string.button_retry)
        HistoryUtil.saveHistory(History(title = title, data = name, type = HistoryUtil.TYPE_FORUM), async = false)
        tapText("我")
        tapText(history)
        tapText(forums)
        repeat(2) { visit ->
            // A physical tap uses the real history row, navigator, page ViewModels and offline request.
            tapText(title)
            awaitText(retry)
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.onNodeWithText(target.getString(R.string.text_load_failed)).assertIsDisplayed()
            if (visit == 0) {
                capture("forum-missing")
                tapText(retry)
                awaitText(retry)
            }
            compose.onNodeWithContentDescription(target.getString(R.string.button_back)).performTouchInput { click() }
            awaitText(forums)
        }
        // Guest API traffic is unavailable in the emulator. Exercise the production success state
        // with a decoded forum containing no sign-in info, avatar or good categories.
        val info = ForumInfo(id = 63L, name = name, member_num = 502, post_num = 1296)
        val state = ForumPartialChange.Load.Success(info, tbs = null).reduce(ForumUiState())
        installContent {
            ProvideShouldLoad(false) {
                ForumContent(name, 0, state, EmptyDestinationsNavigator, {})
            }
        }
        val latest = target.getString(R.string.tab_forum_latest)
        val good = target.getString(R.string.tab_forum_good)
        awaitText(latest)
        compose.onNodeWithText(latest).assertIsDisplayed()
        tapText(good)
        tapText(latest)
        compose.waitForIdle()
        capture("forum-normal")
    }

    private fun photo(name: String): File {
        val file = File(target.cacheDir, "$name.png")
        instrumentation.context.assets.open(file.name).use { source -> file.outputStream().use { source.copyTo(it) } }
        return file
    }

    private fun savedPhotos(): Set<Uri> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val images = mutableSetOf<Uri>()
        target.contentResolver.query(collection, arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.OWNER_PACKAGE_NAME} = ? AND ${MediaStore.Images.Media.IS_PENDING} = 0",
            arrayOf(target.packageName), null)?.use { rows ->
            while (rows.moveToNext()) images.add(ContentUris.withAppendedId(collection, rows.getLong(0)))
        }
        return images
    }

    @Test fun openTwoPhotosAndSaveAndShareTheVisibleImage() {
        val first = photo("trail-map")
        val second = photo("trail-profile")
        val items = listOf(first, second).mapIndexed { index, file ->
            val url = Uri.fromFile(file).toString()
            PicItem(file.nameWithoutExtension, index + 1, url, url, false, 0)
        }
        installContent {
            // The thumbnail handles the tap and starts the production, parcelled PhotoViewActivity.
            NetworkImage(items.first().url, "步道路线图", Modifier.fillMaxWidth().height(180.dp),
                photoViewData = PhotoViewData(picItems = items), skipNetworkCheck = true)
        }
        compose.onNodeWithContentDescription("步道路线图").performTouchInput { click() }
        awaitText("1 / 2")
        capture("gallery-first", Color.rgb(247, 245, 237))
        val metrics = target.resources.displayMetrics
        val x = metrics.widthPixels
        val y = metrics.heightPixels / 2
        val swipe = device.executeShellCommand("input swipe ${x * 4 / 5} $y ${x / 5} $y 340")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(swipe).use { it.readBytes() }
        awaitText("2 / 2")
        capture("gallery-second", Color.rgb(36, 75, 71))

        val share = compose.onNodeWithContentDescription(target.getString(R.string.title_share_pic))
        val save = compose.onNodeWithContentDescription(target.getString(R.string.desc_download_pic))
        val shareBounds = share.fetchSemanticsNode().touchBoundsInRoot
        val saveBounds = save.fetchSemanticsNode().touchBoundsInRoot
        val screenHeight = checkNotNull(device.takeScreenshot()).let { frame -> frame.height.also { frame.recycle() } }
        val density = metrics.density
        for (button in listOf(shareBounds, saveBounds)) {
            assertTrue("Button touch width $button", button.width / density in 47f..51f)
            assertTrue("Button touch height $button", button.height / density in 47f..51f)
            assertTrue("Controls should sit near the bottom", button.top > screenHeight - 150 * density && button.bottom <= screenHeight)
        }
        assertTrue("Button targets overlap", shareBounds.right <= saveBounds.left + 1 && abs(shareBounds.top - saveBounds.top) <= 1)
        File(directory, "gallery-controls.tsv").writeText(
            "control\twidth_dp\theight_dp\tbottom_gap_dp\n" +
                "share\t${shareBounds.width / density}\t${shareBounds.height / density}\t${(screenHeight - shareBounds.bottom) / density}\n" +
                "save\t${saveBounds.width / density}\t${saveBounds.height / density}\t${(screenHeight - saveBounds.bottom) / density}\n"
        )

        val before = savedPhotos()
        save.performTouchInput { click() }
        compose.waitUntil(15_000) { (savedPhotos() - before).isNotEmpty() }
        val saved = (savedPhotos() - before).single()
        try {
            val expected = second.readBytes()
            assertEquals("image/png", target.contentResolver.getType(saved))
            assertArrayEquals(expected, target.contentResolver.openInputStream(saved)!!.use { it.readBytes() })

            val observed = AtomicReference<Intent>()
            val monitor = object : Instrumentation.ActivityMonitor() {
                override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                    if (intent.action == Intent.ACTION_CHOOSER) observed.set(intent)
                    return null // Let Android launch the real chooser.
                }
            }
            instrumentation.addMonitor(monitor)
            try {
                share.performTouchInput { click() }
                val end = SystemClock.uptimeMillis() + 15_000
                val chooserPackage = Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png"), null)
                    .resolveActivity(target.packageManager).packageName
                while (device.rootInActiveWindow?.packageName != chooserPackage && SystemClock.uptimeMillis() < end) {
                    SystemClock.sleep(100)
                }
                assertEquals("System share sheet foreground", chooserPackage, device.rootInActiveWindow?.packageName?.toString())
                val send = checkNotNull(observed.get()?.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java))
                assertEquals(Intent.ACTION_SEND, send.action)
                assertEquals("image/png", send.type)
                assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                val stream = checkNotNull(send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
                assertEquals("content", stream.scheme)
                assertArrayEquals(expected, target.contentResolver.openInputStream(stream)!!.use { it.readBytes() })
                device.waitForIdle(350, 5_000)
                capture("gallery-share")
            } finally {
                instrumentation.removeMonitor(monitor)
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            }
            awaitText("2 / 2")
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithContentDescription("步道路线图").assertIsDisplayed()
        } finally {
            target.contentResolver.delete(saved, null, null)
        }
    }
}
