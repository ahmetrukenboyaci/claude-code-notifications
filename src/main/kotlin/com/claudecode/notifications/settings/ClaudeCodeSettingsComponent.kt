package com.claudecode.notifications.settings

import com.claudecode.notifications.services.HookInstallerService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class ClaudeCodeSettingsComponent {

    val panel: JPanel
    val enabledCheckBox = JBCheckBox("Enable Claude Code Notifications")
    val showBalloonsCheckBox = JBCheckBox("Show IDE balloon notifications")
    val showSystemNotificationsCheckBox = JBCheckBox("Show macOS system notifications")
    val playSoundsCheckBox = JBCheckBox("Play notification sounds")
    val notifyOnQuestionCheckBox = JBCheckBox("Notify when Claude asks a question")
    val notifyOnCompletionCheckBox = JBCheckBox("Notify when Claude completes a task")
    val notifyOnErrorCheckBox = JBCheckBox("Notify when a tool encounters an error")

    private val hookStatusLabel = JLabel()
    private val installHooksButton = JButton("Install / Update Hooks")

    init {
        updateHookStatus()

        installHooksButton.addActionListener {
            val installer = ApplicationManager.getApplication().getService(HookInstallerService::class.java)
            val result = installer.installHooks()
            if (result.success) {
                hookStatusLabel.text = "Hooks installed"
                hookStatusLabel.foreground = Color(0, 128, 0)
            } else {
                hookStatusLabel.text = "Installation failed: ${result.message}"
                hookStatusLabel.foreground = Color.RED
            }
        }

        val hookPanel = JPanel(BorderLayout(8, 0))
        hookPanel.add(installHooksButton, BorderLayout.WEST)
        hookPanel.add(hookStatusLabel, BorderLayout.CENTER)

        panel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox)
            .addSeparator()
            .addComponent(showBalloonsCheckBox)
            .addComponent(showSystemNotificationsCheckBox)
            .addComponent(playSoundsCheckBox)
            .addSeparator()
            .addComponent(notifyOnQuestionCheckBox)
            .addComponent(notifyOnCompletionCheckBox)
            .addComponent(notifyOnErrorCheckBox)
            .addSeparator()
            .addLabeledComponent("Hook Status:", hookPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun updateHookStatus() {
        val installer = ApplicationManager.getApplication().getService(HookInstallerService::class.java)
        if (installer.areHooksInstalled()) {
            hookStatusLabel.text = "Hooks installed"
            hookStatusLabel.foreground = Color(0, 128, 0)
        } else {
            hookStatusLabel.text = "Hooks not installed"
            hookStatusLabel.foreground = Color.RED
        }
    }
}
