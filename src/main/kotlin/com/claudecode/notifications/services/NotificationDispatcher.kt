package com.claudecode.notifications.services

import com.claudecode.notifications.model.ClaudeCodeEvent
import com.claudecode.notifications.model.EventSeverity
import com.claudecode.notifications.model.EventType
import com.claudecode.notifications.settings.ClaudeCodeNotificationSettings
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import java.io.File

@Service(Service.Level.APP)
class NotificationDispatcher {

    private val logger = Logger.getInstance(NotificationDispatcher::class.java)

    fun dispatch(event: ClaudeCodeEvent) {
        val settings = ClaudeCodeNotificationSettings.getInstance().state
        if (!settings.enabled) return

        if (!shouldNotify(event, settings)) return

        ApplicationManager.getApplication().invokeLater {
            if (settings.showBalloons) {
                showBalloon(event)
            }
        }

        val systemNotificationService = ApplicationManager.getApplication()
            .getService(SystemNotificationService::class.java)
        systemNotificationService.showNotification(event)
    }

    private fun shouldNotify(event: ClaudeCodeEvent, settings: ClaudeCodeNotificationSettings.State): Boolean {
        return when (event.type) {
            EventType.QUESTION -> settings.notifyOnQuestion
            EventType.COMPLETION -> settings.notifyOnCompletion
            EventType.ERROR -> settings.notifyOnError
        }
    }

    private fun showBalloon(event: ClaudeCodeEvent) {
        val notificationType = when (event.severity) {
            EventSeverity.INFO -> NotificationType.INFORMATION
            EventSeverity.WARNING -> NotificationType.WARNING
            EventSeverity.ERROR -> NotificationType.ERROR
        }

        val project = findProjectByCwd(event.cwd)

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("ClaudeCodeNotifications")
            .createNotification(event.title, event.message, notificationType)

        if (event.type == EventType.QUESTION) {
            notification.addAction(FocusTerminalAction())
        }

        if (event.severity == EventSeverity.ERROR) {
            notification.isImportant = true
        }

        notification.notify(project)
    }

    private fun findProjectByCwd(cwd: String): com.intellij.openapi.project.Project? {
        if (cwd.isBlank()) return null
        val cwdFile = File(cwd)
        return ProjectManager.getInstance().openProjects.firstOrNull { project ->
            val basePath = project.basePath ?: return@firstOrNull false
            cwdFile.absolutePath.startsWith(basePath)
        }
    }

    private class FocusTerminalAction : com.intellij.notification.NotificationAction("Focus Terminal") {
        override fun actionPerformed(
            e: com.intellij.openapi.actionSystem.AnActionEvent,
            notification: com.intellij.notification.Notification
        ) {
            val project = e.project ?: return
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
            toolWindow?.show()
            notification.expire()
        }
    }
}
