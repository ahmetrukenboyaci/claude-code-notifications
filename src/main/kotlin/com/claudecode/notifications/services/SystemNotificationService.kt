package com.claudecode.notifications.services

import com.claudecode.notifications.model.ClaudeCodeEvent
import com.claudecode.notifications.settings.ClaudeCodeNotificationSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class SystemNotificationService {

    private val logger = Logger.getInstance(SystemNotificationService::class.java)

    fun showNotification(event: ClaudeCodeEvent) {
        val settings = ClaudeCodeNotificationSettings.getInstance().state
        if (!settings.showSystemNotifications) return

        try {
            val title = escapeAppleScript(event.title)
            val message = escapeAppleScript(event.message)
            val soundClause = if (settings.playSounds) " sound name \"Glass\"" else ""

            val script = """display notification "$message" with title "$title" subtitle "Claude Code"$soundClause"""

            val process = ProcessBuilder("osascript", "-e", script)
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(5, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                logger.warn("osascript timed out for event: ${event.type}")
            }
        } catch (e: Exception) {
            logger.warn("Failed to show system notification", e)
        }
    }

    private fun escapeAppleScript(text: String): String {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
