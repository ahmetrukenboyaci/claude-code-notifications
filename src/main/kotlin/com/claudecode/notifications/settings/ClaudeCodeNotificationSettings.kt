package com.claudecode.notifications.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "ClaudeCodeNotificationSettings",
    storages = [Storage("ClaudeCodeNotifications.xml")]
)
class ClaudeCodeNotificationSettings : PersistentStateComponent<ClaudeCodeNotificationSettings.State> {

    data class State(
        var enabled: Boolean = true,
        var showBalloons: Boolean = true,
        var showSystemNotifications: Boolean = true,
        var playSounds: Boolean = true,
        var notifyOnQuestion: Boolean = true,
        var notifyOnCompletion: Boolean = true,
        var notifyOnError: Boolean = true
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(): ClaudeCodeNotificationSettings {
            return ApplicationManager.getApplication().getService(ClaudeCodeNotificationSettings::class.java)
        }
    }
}
