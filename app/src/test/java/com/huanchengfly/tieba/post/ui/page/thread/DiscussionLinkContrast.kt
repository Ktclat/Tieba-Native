package com.huanchengfly.tieba.post.ui.page.thread

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertTrue
import kotlin.math.ceil

/** Measure the link glyphs against the surface actually drawn underneath them. */
internal fun assertDiscussionLinkContrast(view: View, node: SemanticsNodeInteraction, link: String) {
    val origin = node.fetchSemanticsNode().positionInRoot
    node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { read ->
        val layouts = mutableListOf<TextLayoutResult>()
        check(read(layouts))
        val layout = layouts.single()
        val start = layout.layoutInput.text.text.indexOf(link)
        require(start >= 0)
        val pixels = mutableMapOf<Int, Int>()
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(image))
            for (index in start until start + link.length) {
                val bounds = layout.getBoundingBox(index).translate(origin)
                for (y in ceil(bounds.top).toInt() until bounds.bottom.toInt()) {
                    for (x in ceil(bounds.left).toInt() until bounds.right.toInt()) {
                        val color = image.getPixel(x, y)
                        pixels[color] = (pixels[color] ?: 0) + 1
                    }
                }
            }
        } finally {
            image.recycle()
        }
        val surface = pixels.maxBy { it.value }.key
        val readablePixels = pixels.filterKeys { ColorUtils.calculateContrast(it, surface) >= 4.5 }
        val maxContrast = pixels.keys.maxOf { ColorUtils.calculateContrast(it, surface) }
        assertTrue(
            "Expected readable glyph strokes for $link on #${Integer.toHexString(surface)}, maximum contrast $maxContrast",
            readablePixels.values.sum() >= link.length * 2,
        )
    }
}
