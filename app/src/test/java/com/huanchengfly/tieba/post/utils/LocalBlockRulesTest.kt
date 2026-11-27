package com.huanchengfly.tieba.post.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBlockRulesTest {
    @Test fun matchesSubstringsCaseInsensitivelyWithIndependentTargets() {
        val rules = LocalBlockRules.fromText(
            forum = "^摄影$\r\n\r\n(?:Pixel|安卓)",
            user = "^推广\\d+$\n机器人",
            comment = "返利|加群\n(?s)免费.*联系",
        )
        assertEquals(2, rules.count(BlockRuleTarget.FORUM))
        assertTrue(rules.blocksForum("摄影"))
        assertFalse(rules.blocksForum("胶片摄影"))
        assertTrue(rules.blocksForum("GooglePIXEL"))
        assertTrue(rules.blocksUser("推广08", "群友"))
        assertTrue(rules.blocksUser("", null, "某机器人账号"))
        assertTrue(rules.blocksComment("私信加群"))
        assertTrue(rules.blocksComment("免费体验\n请联系"))
        assertFalse(rules.blocksUser("摄影", "返利"))
        assertFalse(rules.blocksForum("机器人"))
        assertFalse(rules.blocksComment("推广08"))
        assertFalse(rules.blocksComment(null))
        val empty = LocalBlockRules.fromText(user = "^$", comment = "^$")
        assertTrue(empty.blocksComment(""))
        assertFalse(empty.blocksComment(null))
        assertFalse(empty.blocksUser("", null))
    }

    @Test fun reportsThePhysicalLineAndKeepsValidRestoredRulesUsable() {
        val raw = "\r\n^互关$\r\n[\r\n回复[0-9]+"
        assertEquals(3, LocalBlockRules.validate(raw)?.line)
        assertTrue(LocalBlockRules.validate(raw)?.message?.isNotBlank() == true)
        assertEquals(1, LocalBlockRules.validate("(?=unsupported)")?.line)
        assertEquals(1, LocalBlockRules.validate("""(a)\1""")?.line)
        assertNull(LocalBlockRules.validate(" \n\t"))
        val rules = LocalBlockRules.fromText(comment = raw)
        assertEquals(raw, rules.text(BlockRuleTarget.COMMENT))
        assertEquals(2, rules.count(BlockRuleTarget.COMMENT))
        assertTrue(rules.blocksComment("互关"))
        assertTrue(rules.blocksComment("回复23"))
        assertFalse(rules.blocksComment("这是一条正常回复"))
        assertTrue(LocalBlockRules.fromText(comment = "\n ").isEmpty)
    }

    @Test fun forumRulesMatchTheDisplayedTitleAndPreserveActualNamesEndingInBa() {
        val rules = LocalBlockRules.fromText(forum = "^摄影吧$\n^网吧吧$\n^显示别名$")
        assertTrue(rules.blocksForum("摄影"))
        assertTrue(rules.blocksForum("网吧"))
        assertTrue(rules.blocksForum("原名", "显示别名"))
        assertFalse(rules.blocksForum("风光摄影"))
        assertFalse(LocalBlockRules.fromText(forum = "^网$").blocksForum("网吧"))
        assertFalse(LocalBlockRules.fromText(forum = "^吧$").blocksForum(""))
    }

    @Test(timeout = 5_000) fun nestedRepetitionTerminatesOnALongNonMatchingComment() {
        val rules = LocalBlockRules.fromText(comment = "^(a+)+b$")
        val longText = "a".repeat(100_000)
        assertFalse(rules.blocksComment(longText + "c"))
        assertTrue(rules.blocksComment(longText + "b"))
    }
}
