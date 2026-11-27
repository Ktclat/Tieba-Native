package com.huanchengfly.tieba.post.utils

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoExportTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun shareUsesImageContentForMimeAndGivesEachRequestItsOwnReadableFile() = runBlocking {
        val source = File(context.cacheDir, "mislabelled.jpg")
        source.outputStream().use { output ->
            Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        val first = ImageUtil.preparePhotoShare(context, Uri.fromFile(source).toString())
        val second = ImageUtil.preparePhotoShare(context, Uri.fromFile(source).toString())
        assertNotEquals(first, second)
        assertEquals("content", first.scheme)
        // Robolectric starts with an empty system MIME registry. FileProvider queries it by extension.
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypeMapping("png", "image/png")
        assertEquals("image/png", context.contentResolver.getType(first))
        assertArrayEquals(source.readBytes(), context.contentResolver.openInputStream(first)!!.use { it.readBytes() })
    }

    @Test fun failedDownloadCannotReturnAShareUriForAnEmptyFile() {
        val source = File(context.cacheDir, "unavailable.jpg").apply { writeText("Photo unavailable") }
        assertThrows(IOException::class.java) {
            runBlocking { ImageUtil.preparePhotoShare(context, Uri.fromFile(source).toString()) }
        }
    }
}
