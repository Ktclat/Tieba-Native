package com.huanchengfly.tieba.post.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

object LocalBlockRuleStore {
    private val forumKey = stringPreferencesKey("local_block_regex_forum")
    private val userKey = stringPreferencesKey("local_block_regex_user")
    private val commentKey = stringPreferencesKey("local_block_regex_comment")

    fun rules(context: Context): Flow<LocalBlockRules> = rules(context.dataStore)

    internal fun rules(store: DataStore<Preferences>): Flow<LocalBlockRules> =
        store.data.map { preferences ->
            Triple(preferences[forumKey].orEmpty(), preferences[userKey].orEmpty(), preferences[commentKey].orEmpty())
        }.distinctUntilChanged().map { (forum, user, comment) ->
            LocalBlockRules.fromText(forum, user, comment)
        }.flowOn(Dispatchers.Default)

    suspend fun appendCommentLiteral(context: Context, selection: String): Boolean =
        appendCommentLiteral(context.dataStore, selection)

    internal suspend fun appendCommentLiteral(store: DataStore<Preferences>, selection: String): Boolean {
        val pattern = LocalBlockRules.commentLiteralPattern(selection)
        var added = false
        store.edit { preferences ->
            val existing = preferences[commentKey].orEmpty()
            if (existing.lineSequence().none { it == pattern }) {
                val needsSeparator = existing.isNotEmpty() && !existing.endsWith('\n') && !existing.endsWith('\r')
                preferences[commentKey] = if (needsSeparator) "$existing\n$pattern" else existing + pattern
                added = true
            }
        }
        return added
    }

    suspend fun save(context: Context, target: BlockRuleTarget, text: String) =
        save(context.dataStore, target, text)

    internal suspend fun save(store: DataStore<Preferences>, target: BlockRuleTarget, text: String) {
        val error = withContext(Dispatchers.Default) { LocalBlockRules.validate(text) }
        require(error == null) { "Line ${error?.line}: ${error?.message}" }
        val key = when (target) {
            BlockRuleTarget.FORUM -> forumKey
            BlockRuleTarget.USER -> userKey
            BlockRuleTarget.COMMENT -> commentKey
        }
        store.edit {
            if (text.isEmpty()) it.remove(key) else it[key] = text
        }
    }
}
