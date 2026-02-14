package com.claudecode.notifications.actions

import com.claudecode.notifications.services.HookInstallerService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager

class InstallHooksAction : AnAction("Install Claude Code Hooks") {

    override fun actionPerformed(e: AnActionEvent) {
        val installer = ApplicationManager.getApplication().getService(HookInstallerService::class.java)
        val result = installer.installHooks()

        val type = if (result.success) NotificationType.INFORMATION else NotificationType.ERROR
        val title = if (result.success) "Hooks Installed" else "Hook Installation Failed"

        NotificationGroupManager.getInstance()
            .getNotificationGroup("ClaudeCodeNotifications")
            .createNotification(title, result.message, type)
            .notify(e.project)
    }
}
