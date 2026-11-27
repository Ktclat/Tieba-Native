package com.huanchengfly.tieba.post.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
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

    private fun changedFraction(a: Bitmap, b: Bitmap, arrowCenter: PointF? = null): Float {
        check(a.width == b.width && a.height == b.height)
        var changed = 0
        var samples = 0
        val arrowRadius = 26 * context.resources.displayMetrics.density
        // Compare the full content width. Only the held frame has a SystemUI arrow.
        for (y in a.height / 9 until a.height * 9 / 10 step 8) {
            for (x in 0 until a.width step 8) {
                if (arrowCenter != null) {
                    val dx = x - arrowCenter.x
                    val dy = y - arrowCenter.y
                    if (dx * dx + dy * dy <= arrowRadius * arrowRadius) continue
                }
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
            // In this Pixel API 35 profile the 48dp system arrow is centered 40dp from the
            // active edge and 64dp above the touch. Allow 2dp for its settling and antialiasing.
            val dp = context.resources.displayMetrics.density
            val arrowCenter = PointF(if (left) 40 * dp else before.width - 40 * dp, y - 64 * dp)
            val duringChange = changedFraction(before, during, arrowCenter)
            val settled = changedFraction(before, after)
            val metrics = File(context.filesDir, "phone-review/gestures.tsv")
            if (!metrics.exists()) metrics.writeText("gesture\tdrag_changed_fraction\tsettled_changed_fraction\n")
            metrics.appendText("$name\t$duringChange\t$settled\n")
            // SystemUI draws its own edge arrow, but the sampled page must stay in place until release.
            assertTrue("$name moved content while held ($duringChange)", duringChange < 0.001f)
            if (cancel) {
                assertTrue("$name changed content on cancellation ($settled)", settled < 0.001f)
            } else {
                assertTrue("$name did not change destination on release ($settled)", settled > 0.005f)
            }
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
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitLabel("请先登录")
            tap("主题选择")
            tap("自定义颜色")
            awaitLabel("设置自定义主题")
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitLabel("跟随系统壁纸")
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitLabel("请先登录")
        }
    }
}
