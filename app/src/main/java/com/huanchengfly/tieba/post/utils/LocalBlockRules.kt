package com.huanchengfly.tieba.post.utils

import com.google.re2j.Pattern
import com.google.re2j.PatternSyntaxException

enum class BlockRuleTarget { FORUM, USER, COMMENT }

data class BlockRuleError(val line: Int, val message: String)

/** Immutable snapshot shared by the lists and the rule editors. */
class LocalBlockRules private constructor(private val groups: Map<BlockRuleTarget, RuleGroup>) {
    val isEmpty: Boolean = groups.values.all { it.patterns.isEmpty() }

    fun text(target: BlockRuleTarget): String = groups[target]?.text.orEmpty()

    fun count(target: BlockRuleTarget): Int = groups[target]?.patterns?.size ?: 0

    fun blocksForum(name: String?, displayName: String? = null): Boolean {
        if (count(BlockRuleTarget.FORUM) == 0) return false
        if (!displayName.isNullOrEmpty() && matches(BlockRuleTarget.FORUM, displayName)) return true
        if (name.isNullOrEmpty()) return false
        // The page title appends 吧 to the API name. A raw name can itself end in 吧, e.g. 网吧.
        return matches(BlockRuleTarget.FORUM, name) || matches(BlockRuleTarget.FORUM, name + "吧")
    }

    fun blocksUser(vararg names: String?): Boolean =
        names.any { !it.isNullOrEmpty() && matches(BlockRuleTarget.USER, it) }

    fun blocksComment(body: String?): Boolean = matches(BlockRuleTarget.COMMENT, body)

    private fun matches(target: BlockRuleTarget, value: String?): Boolean =
        value != null && groups[target]?.patterns?.any { it.matcher(value).find() } == true

    companion object {
        val Empty = LocalBlockRules(emptyMap())

        fun validate(text: String): BlockRuleError? = compileGroup(text).firstError

        fun fromText(forum: String = "", user: String = "", comment: String = ""): LocalBlockRules {
            if (forum.isEmpty() && user.isEmpty() && comment.isEmpty()) return Empty
            return LocalBlockRules(
                mapOf(
                    BlockRuleTarget.FORUM to compileGroup(forum),
                    BlockRuleTarget.USER to compileGroup(user),
                    BlockRuleTarget.COMMENT to compileGroup(comment),
                )
            )
        }

        private fun compileGroup(text: String): RuleGroup {
            val patterns = mutableListOf<Pattern>()
            var firstError: BlockRuleError? = null
            text.lineSequence().forEachIndexed { index, line ->
                if (line.isNotBlank()) {
                    try {
                        patterns.add(Pattern.compile(line, Pattern.CASE_INSENSITIVE))
                    } catch (error: PatternSyntaxException) {
                        // Keep valid siblings usable if a restored ruleset contains unsupported syntax.
                        if (firstError == null) firstError = BlockRuleError(index + 1, error.description)
                    }
                }
            }
            return RuleGroup(text, patterns, firstError)
        }
    }

    private class RuleGroup(val text: String, val patterns: List<Pattern>, val firstError: BlockRuleError?)
}
