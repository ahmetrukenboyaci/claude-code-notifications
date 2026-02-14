package com.claudecode.notifications.startup

import com.claudecode.notifications.services.EventFileWatcherService
import com.claudecode.notifications.services.HookInstallerService
import com.claudecode.notifications.settings.ClaudeCodeNotificationSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ClaudeCodeStartupActivity : ProjectActivity {

    private val logger = Logger.getInstance(ClaudeCodeStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        val settings = ClaudeCodeNotificationSettings.getInstance().state
        if (!settings.enabled) {
            logger.info("Claude Code Notifications disabled, skipping startup")
            return
        }

        val installer = ApplicationManager.getApplication().getService(HookInstallerService::class.java)
        if (!installer.areHooksInstalled()) {
            logger.info("Hooks not installed, auto-installing...")
            val result = installer.installHooks()
            if (result.success) {
                logger.info("Hooks auto-installed successfully")
            } else {
                logger.warn("Hook auto-install failed: ${result.message}")
            }
        }

        val watcher = ApplicationManager.getApplication().getService(EventFileWatcherService::class.java)
        watcher.start()

        logger.info("Claude Code Notifications started for project: ${project.name}")
    }
}
