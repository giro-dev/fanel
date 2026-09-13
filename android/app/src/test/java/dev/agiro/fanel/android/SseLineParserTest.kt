package dev.agiro.fanel.android

import dev.agiro.fanel.android.sync.SseLineParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SseLineParserTest {
    @Test
    fun emitsEventNameOnBlankLine() {
        val parser = SseLineParser()
        assertNull(parser.accept("event: shopping"))
        assertNull(parser.accept("data: abc"))
        assertEquals("shopping", parser.accept(""))
    }

    @Test
    fun ignoresDataOnlyBlocks() {
        val parser = SseLineParser()
        assertNull(parser.accept("data: ping"))
        assertNull(parser.accept(""))
    }

    @Test
    fun handlesMultipleEvents() {
        val parser = SseLineParser()
        parser.accept("event: menu")
        parser.accept("data: h1")
        assertEquals("menu", parser.accept(""))
        parser.accept("event: chores")
        parser.accept("data: h1")
        assertEquals("chores", parser.accept(""))
    }

    @Test
    fun ignoresCommentLines() {
        val parser = SseLineParser()
        parser.accept(": keep-alive")
        assertNull(parser.accept(""))
    }
}
