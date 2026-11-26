package com.huanchengfly.tieba.post.ui.page.history

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryTimeline
import com.huanchengfly.tieba.post.utils.HistoryUtil
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN")
class HistoryTimelineTest {
    private val zone = ZoneId.of("Asia/Hong_Kong")
    private fun timeline() = HistoryTimeline(ApplicationProvider.getApplicationContext(), Locale.SIMPLIFIED_CHINESE, zone)
    private fun ms(value: String) = Instant.parse(value).toEpochMilli()

    @Test fun scrollingAndClockTicksReuseRowsUntilTheirOwnLabelExpires() {
        val now = ms("2026-11-26T06:00:00Z")
        val records = buildList {
            add(History("刚访问", timestamp = now, type = HistoryUtil.TYPE_FORUM))
            repeat(180) { add(History("访问 $it", timestamp = now - (it + 1) * 60_000, type = HistoryUtil.TYPE_FORUM)) }
        }
        val timeline = timeline()
        val first = timeline.group(records, now)
        val same = timeline.group(records, now + 500)
        assertSame(first.today, same.today)
        assertEquals("刚刚", first.today[0].subtitle)
        assertEquals(now + 1_000, first.nextChange)
        val next = timeline.group(records, now + 1_000)
        assertEquals("1 秒前", next.today[0].subtitle)
        for (index in 1 until first.today.size) assertSame(first.today[index], next.today[index])
        val minute = timeline.group(records, now + 60_000)
        assertEquals("1 分钟前", minute.today[0].subtitle)
        assertEquals("2 分钟前", minute.today[1].subtitle)
        assertSame(next.today.last(), minute.today.last())
        assertEquals("刚刚", timeline.group(records, now).today.first().subtitle)
    }

    @Test fun localMidnightMovesPriorYearAndSecondTimestampsToTheCorrectSection() {
        val eve = ms("2026-12-31T15:59:59Z")
        val record = History("跨年", type = HistoryUtil.TYPE_FORUM, timestamp = eve / 1_000)
        val sameFields = record.copy()
        val rows = listOf(record, sameFields)
        val timeline = timeline()
        val before = timeline.group(rows, eve)
        assertEquals(2, before.today.size)
        assertSame(record, before.today[0].visit)
        assertSame(sameFields, before.today[1].visit)
        assertEquals(eve + 1_000, before.nextChange)
        val after = timeline.group(rows, eve + 1_000)
        assertTrue(after.today.isEmpty())
        assertEquals("2026-12-31 23:59", after.earlier.first().subtitle)
    }
}
