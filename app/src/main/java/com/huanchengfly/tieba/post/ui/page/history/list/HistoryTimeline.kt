package com.huanchengfly.tieba.post.ui.page.history.list

import android.content.Context
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.utils.HistoryUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.PriorityQueue

internal data class VisitLabel(val visit: History, val title: String, val menu: String, val subtitle: String, val until: Long)
internal data class HistoryGroups(val today: ImmutableList<VisitLabel>, val earlier: ImmutableList<VisitLabel>, val nextChange: Long)

/** Prepare a loaded result set off the UI thread. Scrolling reuses the labels, including across recycled rows. */
internal class HistoryTimeline(private val context: Context, locale: Locale, private val zone: ZoneId) {
    private val clock = DateTimeFormatter.ofPattern("HH:mm", locale).withZone(zone)
    private val shortDate = DateTimeFormatter.ofPattern("MM-dd HH:mm", locale).withZone(zone)
    private val fullDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale).withZone(zone)
    private var source: List<History>? = null
    private var today: PersistentList<VisitLabel> = persistentListOf()
    private var earlier: PersistentList<VisitLabel> = persistentListOf()
    private var tomorrow = 0L
    private var yearStart = 0L
    private var lastNow = Long.MIN_VALUE
    private data class Due(val index: Int, val time: Long)
    private val due = PriorityQueue(compareBy(Due::time))

    fun group(visits: List<History>, now: Long): HistoryGroups {
        if (visits !== source || now < lastNow || now >= tomorrow) {
            val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            tomorrow = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            yearStart = date.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val todayRows = persistentListOf<VisitLabel>().builder()
            val earlierRows = persistentListOf<VisitLabel>().builder()
            due.clear()
            for (visit in visits) {
                val timestamp = historyMillis(visit.timestamp)
                val isToday = timestamp >= start && timestamp < tomorrow
                val row = label(visit, timestamp, now, isToday)
                if (isToday) {
                    if (row.until < tomorrow) due.add(Due(todayRows.size, row.until))
                    todayRows.add(row)
                } else earlierRows.add(row)
            }
            today = todayRows.build()
            earlier = earlierRows.build()
            source = visits
        } else {
            // Only recent visits change before midnight. A second ticking on the newest record
            // must not reformat, copy or scan the other (potentially many) loaded records.
            while (due.isNotEmpty() && due.peek().time <= now) {
                val index = due.remove().index
                val visit = today[index].visit
                val row = label(visit, historyMillis(visit.timestamp), now, true)
                today = today.set(index, row)
                if (row.until < tomorrow) due.add(Due(index, row.until))
            }
        }
        lastNow = now
        return HistoryGroups(today, earlier, due.peek()?.time ?: tomorrow)
    }

    private fun label(visit: History, timestamp: Long, now: Long, isToday: Boolean): VisitLabel {
        val age = now - timestamp
        val (time, until) = when {
            age < 0 || (isToday && age < 1_000) -> context.getString(R.string.relative_date_after) to (timestamp + 1_000)
            isToday && age < 60_000 -> {
                val seconds = age / 1_000
                context.getString(R.string.relative_date_second, seconds) to (timestamp + (seconds + 1) * 1_000)
            }
            isToday && age < 3_600_000 -> {
                val minutes = age / 60_000
                context.getString(R.string.relative_date_minute, minutes) to (timestamp + (minutes + 1) * 60_000)
            }
            isToday -> context.getString(R.string.relative_date_today, clock.format(Instant.ofEpochMilli(timestamp))) to tomorrow
            timestamp >= yearStart -> shortDate.format(Instant.ofEpochMilli(timestamp)) to tomorrow
            else -> fullDate.format(Instant.ofEpochMilli(timestamp)) to tomorrow
        }
        val title = visit.title.ifBlank { visit.data }
        val subtitle = if (visit.type == HistoryUtil.TYPE_THREAD && !visit.username.isNullOrBlank())
            context.getString(R.string.history_author_time, visit.username, time) else time
        return VisitLabel(visit, title, context.getString(R.string.history_item_actions, title), subtitle, until)
    }

    // Older timestamps may use Unix seconds, while visits written by the current app use milliseconds.
    private fun historyMillis(timestamp: Long) = if (timestamp in 1_000_000_000L..9_999_999_999L) timestamp * 1_000 else timestamp
}
