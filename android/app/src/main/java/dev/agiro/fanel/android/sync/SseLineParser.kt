package dev.agiro.fanel.android.sync

/** Accumulates SSE lines and yields the event name when a blank line closes the event block. */
class SseLineParser {
    private var pendingEvent: String? = null

    fun accept(line: String): String? {
        when {
            line.isEmpty() -> {
                val event = pendingEvent
                pendingEvent = null
                return event
            }
            line.startsWith("event:") -> pendingEvent = line.substring(6).trim()
        }
        return null
    }
}
