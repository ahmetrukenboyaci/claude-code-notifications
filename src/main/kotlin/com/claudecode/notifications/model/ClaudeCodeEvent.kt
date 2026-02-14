package com.claudecode.notifications.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EventType {
    @SerialName("question")
    QUESTION,

    @SerialName("completion")
    COMPLETION,

    @SerialName("error")
    ERROR
}

@Serializable
enum class EventSeverity {
    @SerialName("info")
    INFO,

    @SerialName("warning")
    WARNING,

    @SerialName("error")
    ERROR
}

@Serializable
data class ClaudeCodeEvent(
    val type: EventType,
    val severity: EventSeverity = EventSeverity.INFO,
    val title: String,
    val message: String = "",
    val cwd: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val hookEvent: String = "",
    val sessionId: String = ""
)
