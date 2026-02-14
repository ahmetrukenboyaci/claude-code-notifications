package com.claudecode.notifications.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class ClaudeCodeSettingsConfigurable : Configurable {

    private var component: ClaudeCodeSettingsComponent? = null

    override fun getDisplayName(): String = "Claude Code Notifications"

    override fun createComponent(): JComponent {
        component = ClaudeCodeSettingsComponent()
        return component!!.panel
    }

    override fun isModified(): Boolean {
        val settings = ClaudeCodeNotificationSettings.getInstance().state
        val c = component ?: return false
        return c.enabledCheckBox.isSelected != settings.enabled ||
                c.showBalloonsCheckBox.isSelected != settings.showBalloons ||
                c.showSystemNotificationsCheckBox.isSelected != settings.showSystemNotifications ||
                c.playSoundsCheckBox.isSelected != settings.playSounds ||
                c.notifyOnQuestionCheckBox.isSelected != settings.notifyOnQuestion ||
                c.notifyOnCompletionCheckBox.isSelected != settings.notifyOnCompletion ||
                c.notifyOnErrorCheckBox.isSelected != settings.notifyOnError
    }

    override fun apply() {
        val settings = ClaudeCodeNotificationSettings.getInstance()
        val c = component ?: return
        settings.loadState(
            ClaudeCodeNotificationSettings.State(
                enabled = c.enabledCheckBox.isSelected,
                showBalloons = c.showBalloonsCheckBox.isSelected,
                showSystemNotifications = c.showSystemNotificationsCheckBox.isSelected,
                playSounds = c.playSoundsCheckBox.isSelected,
                notifyOnQuestion = c.notifyOnQuestionCheckBox.isSelected,
                notifyOnCompletion = c.notifyOnCompletionCheckBox.isSelected,
                notifyOnError = c.notifyOnErrorCheckBox.isSelected
            )
        )
    }

    override fun reset() {
        val settings = ClaudeCodeNotificationSettings.getInstance().state
        val c = component ?: return
        c.enabledCheckBox.isSelected = settings.enabled
        c.showBalloonsCheckBox.isSelected = settings.showBalloons
        c.showSystemNotificationsCheckBox.isSelected = settings.showSystemNotifications
        c.playSoundsCheckBox.isSelected = settings.playSounds
        c.notifyOnQuestionCheckBox.isSelected = settings.notifyOnQuestion
        c.notifyOnCompletionCheckBox.isSelected = settings.notifyOnCompletion
        c.notifyOnErrorCheckBox.isSelected = settings.notifyOnError
    }

    override fun disposeUIResources() {
        component = null
    }
}
