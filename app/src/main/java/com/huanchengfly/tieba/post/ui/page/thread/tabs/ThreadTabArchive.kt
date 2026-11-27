package com.huanchengfly.tieba.post.ui.page.thread.tabs

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One preferences file per account. Scrolling updates just that thread's record. */
internal class ThreadTabArchive private constructor(private val prefs: SharedPreferences) {
    fun read(): List<ThreadTab> = prefs.all.mapNotNull { (key, value) ->
        val id = key.toLongOrNull() ?: return@mapNotNull null
        if (value !is String) return@mapNotNull null
        try {
            json.decodeFromString<ThreadTab>(value).takeIf { it.threadId == id && id > 0 }
        } catch (_: SerializationException) {
            null
        }
    }.sortedByDescending { it.savedOrder }

    fun put(tab: ThreadTab) {
        prefs.edit().putString(tab.threadId.toString(), json.encodeToString(tab)).apply()
    }

    fun remove(id: Long) {
        prefs.edit().remove(id.toString()).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val sessions = mutableMapOf<SharedPreferences, ThreadTabs>()

        fun open(context: Context, accountId: String?): ThreadTabArchive {
            val owner = accountId?.takeIf { it.isNotBlank() }?.let { "user_${Uri.encode(it)}" } ?: "guest"
            return ThreadTabArchive(context.getSharedPreferences("thread_tabs_$owner", Context.MODE_PRIVATE))
        }

        // Duplicate activity windows for an account share their immediate in-memory changes.
        fun session(context: Context, accountId: String?): ThreadTabs {
            val archive = open(context, accountId)
            return sessions.getOrPut(archive.prefs) { ThreadTabs(archive) }
        }
    }
}
