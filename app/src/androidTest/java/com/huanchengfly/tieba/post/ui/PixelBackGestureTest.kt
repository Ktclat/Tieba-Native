package com.huanchengfly.tieba.post.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.huanchengfly.tieba.post.MainActivityV2
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Drive SystemUI's edge recognizer with one continuous, real touch stream per gesture. */
@RunWith(AndroidJUnit4::class)
class PixelBackGestureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device get() = instrumentation.uiAutomation
    private val context get() = instrumentation.targetContext

    private fun awaitLabel(label: String): Rect {
        fun lookup(node: AccessibilityNodeInfo): Rect? {
            if (node.isVisibleToUser && node.packageName == context.packageName &&
                listOf(node.text, node.contentDescription).any { value ->
                    value?.toString()?.split(',', '，', '\n')?.any { it.trim() == label } == true
                }
            ) return Rect().also(node::getBoundsInScreen)
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                lookup(child)?.let { return it }
            }
            return null
        }
        val deadline = SystemClock.uptimeMillis() + 20_000
        do {
            device.rootInActiveWindow?.let { root -> lookup(root)?.let { return it } }
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        capture("unexpected-foreground").recycle()
        error("Could not find $label")
    }

    private fun touch(down: Long, action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            check(device.injectInputEvent(event, true)) { "Input injection failed" }
        } finally {
            event.recycle()
        }
    }

    private fun tap(label: String) {
        val bounds = awaitLabel(label)
        val down = SystemClock.uptimeMillis()
        touch(down, MotionEvent.ACTION_DOWN, bounds.exactCenterX(), bounds.exactCenterY())
        touch(down, MotionEvent.ACTION_UP, bounds.exactCenterX(), bounds.exactCenterY())
    }

    private fun capture(name: String): Bitmap {
        val raw = checkNotNull(device.takeScreenshot())
        val bitmap = if (raw.config == Bitmap.Config.HARDWARE) {
            raw.copy(Bitmap.Config.ARGB_8888, false).also { raw.recycle() }
        } else raw
        val directory = File(context.filesDir, "phone-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    private fun changedFraction(a: Bitmap, b: Bitmap): Float {
        check(a.width == b.width && a.height == b.height)
        var changed = 0
        var samples = 0
        // Exclude status/navigation bars and the system's back arrow at either edge.
        for (y in a.height / 9 until a.height * 9 / 10 step 8) {
            for (x in a.width / 12 until a.width * 11 / 12 step 8) {
                val old = a.getPixel(x, y)
                val new = b.getPixel(x, y)
                val difference = abs(Color.red(old) - Color.red(new)) + abs(Color.green(old) - Color.green(new)) +
                    abs(Color.blue(old) - Color.blue(new))
                if (difference > 72) changed++
                samples++
            }
        }
        return changed.toFloat() / samples
    }

    private fun outgoingOffset(before: Bitmap, during: Bitmap, left: Boolean, name: String): Int {
        // Match outgoing text/icon edges in the app content. System bars, gesture arrows, flat
        // backgrounds and the shared bottom bar cannot satisfy the translation assertion.
        val w = before.width / 4
        val h = before.height / 4
        fun luminance(frame: Bitmap): IntArray {
            val scaled = Bitmap.createScaledBitmap(frame, w, h, true)
            val pixels = IntArray(w * h)
            scaled.getPixels(pixels, 0, w, 0, 0, w, h)
            scaled.recycle()
            return IntArray(pixels.size) { i ->
                val c = pixels[i]
                (Color.red(c) + 2 * Color.green(c) + Color.blue(c)) / 4
            }
        }
        val source = luminance(before)
        val drag = luminance(during)
        val anchors = ArrayList<Int>()
        for (y in h / 12 until h * 4 / 5 step 2) {
            for (x in 3 until w - 3) {
                val i = y * w + x
                if (maxOf(source[i - 2], source[i + 2], source[i - 2 * w], source[i + 2 * w]) - source[i] > 40) {
                    anchors += i
                }
            }
        }
        fun best(sign: Int): Pair<Float, Int> {
            var best = 0f to 0
            for (offset in w / 30..w * 9 / 10) {
                val dx = offset * sign
                var samples = 0
                var matches = 0
                for (i in anchors) {
                    if (i % w + dx !in 3 until w - 3) continue
                    samples++
                    if (abs(source[i] - drag[i + dx]) < 34) matches++
                }
                if (samples >= 45 && matches.toFloat() / samples > best.first) {
                    best = matches.toFloat() / samples to dx
                }
            }
            return best
        }
        val expected = best(if (left) 1 else -1)
        val opposite = best(if (left) -1 else 1)
        assertTrue("$name outgoing content direction: expected=$expected, opposite=$opposite",
            expected.first > 0.42f && expected.first > opposite.first + 0.04f)
        return expected.second * 4
    }

    private fun gesture(name: String, left: Boolean, cancel: Boolean, source: String, destination: String) {
        awaitLabel(source)
        SystemClock.sleep(500)
        val before = capture("$name-before")
        val edge = if (left) 2f else before.width - 3f
        val direction = if (left) 1f else -1f
        val distance = before.width * 0.39f * direction
        val y = before.height * 0.48f
        val down = SystemClock.uptimeMillis()
        touch(down, MotionEvent.ACTION_DOWN, edge, y)
        fun move(step: Int) {
            touch(down, MotionEvent.ACTION_MOVE, edge + distance * step / 16, y)
            SystemClock.sleep(12)
        }
        for (step in 1..16) move(step)
        SystemClock.sleep(100)
        val during = capture("$name-drag")
        if (cancel) for (step in 15 downTo 0) move(step)
        touch(down, MotionEvent.ACTION_UP, if (cancel) edge else edge + distance, y)
        awaitLabel(if (cancel) source else destination)
        SystemClock.sleep(650)
        val after = capture("$name-settled")
        try {
            val preview = changedFraction(before, during)
            val settled = changedFraction(before, after)
            assertTrue("$name produced no content preview ($preview)", preview > 0.005f)
            val offset = outgoingOffset(before, during, left, name)
            // A finger held at 39% of the viewport should leave both destinations visible.
            val fraction = abs(offset).toFloat() / before.width
            assertTrue("$name preview moved $fraction of the viewport for a 39% drag",
                fraction in 0.16f..0.67f)
            val metrics = File(context.filesDir, "phone-review/gestures.tsv")
            if (!metrics.exists()) metrics.writeText("gesture\toutgoing_offset_px\tdrag_changed_fraction\tsettled_changed_fraction\n")
            metrics.appendText("$name\t$offset\t$preview\t$settled\n")
            if (cancel) assertTrue("$name did not restore original content ($settled)", settled < 0.02f)
        } finally {
            before.recycle()
            during.recycle()
            after.recycle()
        }
    }

    @Test fun cancelAndCommitFromOppositeEdgesOnTabsAndNestedPages() {
        ActivityScenario.launch(MainActivityV2::class.java).use {
            awaitLabel("随便看看")
            tap("我")
            gesture("tab-left-cancel", left = true, cancel = true, source = "请先登录", destination = "随便看看")
            gesture("tab-right-commit", left = false, cancel = false, source = "请先登录", destination = "随便看看")
            tap("我")
            tap("设置")
            tap("个性化")
            gesture("page-right-cancel", left = false, cancel = true, source = "字体大小", destination = "使用习惯")
            gesture("page-left-commit", left = true, cancel = false, source = "字体大小", destination = "使用习惯")
            tap("返回")
            awaitLabel("请先登录")
        }
    }
}
