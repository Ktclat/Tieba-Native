package com.huanchengfly.tieba.post.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import java.io.File
import org.junit.Assert.assertTrue

/** Draws the Robolectric view for visual review using native graphics. */
internal fun ComposeContentTestRule.captureReviewImage(view: View, name: String) {
    val image = runOnIdle {
        check(view.width > 0 && view.height > 0)
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
            view.draw(Canvas(it))
        }
    }
    try {
        val samples = mutableSetOf<Int>()
        for (y in 0 until image.height step 7) {
            for (x in 0 until image.width step 7) samples.add(image.getPixel(x, y))
        }
        assertTrue("Review image must contain rendered content", samples.size > 4)
        val directory = File("build/reports/ui-screenshots").apply { mkdirs() }
        File(directory, name).outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    } finally {
        image.recycle()
    }
}
