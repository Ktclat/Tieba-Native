package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.utils.extension.findFlow
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.litepal.LitePal
import org.litepal.crud.async.FindMultiExecutor
import org.litepal.extension.deleteAll
import org.litepal.extension.find
import org.litepal.extension.findAsync

internal data class HistoryCursor(val timestamp: Long, val count: Int, val id: Long)

internal data class HistoryPageResult(val rows: List<History>, val next: HistoryCursor?)

object HistoryUtil {
    const val PAGE_SIZE = 100
    const val TYPE_FORUM = 1
    const val TYPE_THREAD = 2
    private const val ORDER = "timestamp DESC, count DESC, id DESC"

    // Clearing history must also clear visits that were already queued when the user confirmed.
    private val writer = Executors.newSingleThreadExecutor()
    private val clears = MutableStateFlow(0L)
    val clearVersion = clears.asStateFlow()

    fun deleteAll() {
        writer.submit {
            LitePal.deleteAll<History>()
            clears.value += 1L
        }.get()
    }

    internal fun delete(type: Int, id: Long) {
        writer.submit { LitePal.deleteAll<History>("type = ? AND id = ?", type.toString(), id.toString()) }.get()
    }

    @JvmOverloads
    fun saveHistory(history: History, async: Boolean = true) {
        val write = writer.submit {
            val existing = LitePal.where("type = ? AND data = ?", history.type.toString(), history.data)
                .findFirst(History::class.java)
            val visit = history.copy(
                timestamp = System.currentTimeMillis(),
                count = (existing?.count ?: 0) + 1,
            )
            if (existing == null) visit.saveThrows() else visit.update(existing.id)
        }
        if (!async) write.get()
    }

    val all: List<History>
        get() = LitePal.order(ORDER).limit(PAGE_SIZE).find<History>()

    fun getAll(type: Int): List<History> =
        LitePal.where("type = ?", type.toString()).order(ORDER).limit(PAGE_SIZE).find<History>()

    fun getAllAsync(type: Int): FindMultiExecutor<History> =
        LitePal.where("type = ?", type.toString()).order(ORDER).limit(PAGE_SIZE).findAsync<History>()

    fun getFlow(type: Int, page: Int): Flow<List<History>> =
        LitePal.where("type = ?", type.toString()).order(ORDER)
            .limit(PAGE_SIZE).offset(page * PAGE_SIZE).findFlow()

    internal fun search(type: Int, keyword: String, after: HistoryCursor?): HistoryPageResult {
        val query = keyword.trim()
        val predicates = mutableListOf("type = ?")
        val args = mutableListOf(type.toString())
        if (query.isNotEmpty()) {
            // instr treats %, _, quotes and backslashes as part of the search term.
            if (type == TYPE_THREAD) {
                predicates += "(instr(lower(title), lower(?)) > 0 OR instr(lower(username), lower(?)) > 0 OR data = ?)"
                args += listOf(query, query, query)
            } else {
                predicates += "instr(lower(data), lower(?)) > 0"
                args += query
            }
        }
        if (after != null) {
            predicates += "(timestamp < ? OR (timestamp = ? AND count < ?) OR (timestamp = ? AND count = ? AND id < ?))"
            args += listOf(
                after.timestamp.toString(), after.timestamp.toString(), after.count.toString(),
                after.timestamp.toString(), after.count.toString(), after.id.toString(),
            )
        }
        val found = LitePal.where(predicates.joinToString(" AND "), *args.toTypedArray())
            .order(ORDER)
            .limit(PAGE_SIZE + 1)
            .find<History>()
        val rows = found.take(PAGE_SIZE)
        val next = if (found.size > PAGE_SIZE) {
            rows.last().let { HistoryCursor(it.timestamp, it.count, it.id) }
        } else null
        return HistoryPageResult(rows, next)
    }
}
