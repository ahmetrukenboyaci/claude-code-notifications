package com.claudecode.notifications.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

data class InstallResult(val success: Boolean, val message: String)

@Service(Service.Level.APP)
class HookInstallerService {

    private val logger = Logger.getInstance(HookInstallerService::class.java)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val notifDir: Path = Paths.get(System.getProperty("user.home"), ".claude-code-notifications")
    private val scriptPath: Path = notifDir.resolve("claude-code-notify.sh")
    private val claudeSettingsPath: Path = Paths.get(System.getProperty("user.home"), ".claude", "settings.json")

    fun installHooks(): InstallResult {
        try {
            Files.createDirectories(notifDir)

            extractHookScript()
            mergeHooksIntoSettings()

            return InstallResult(true, "Hooks installed successfully")
        } catch (e: Exception) {
            logger.error("Failed to install hooks", e)
            return InstallResult(false, e.message ?: "Unknown error")
        }
    }

    fun areHooksInstalled(): Boolean {
        if (!Files.exists(scriptPath)) return false
        if (!Files.exists(claudeSettingsPath)) return false

        try {
            val content = Files.readString(claudeSettingsPath)
            val root = json.parseToJsonElement(content).jsonObject
            val hooks = root["hooks"]?.jsonObject ?: return false
            return hooks.containsKey("Notification") && hooks.containsKey("Stop")
        } catch (e: Exception) {
            return false
        }
    }

    private fun extractHookScript() {
        val scriptContent = javaClass.getResourceAsStream("/hooks/claude-code-notify.sh")
            ?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Hook script not found in plugin resources")

        val tmpFile = notifDir.resolve(".tmp-hook-script-${System.currentTimeMillis()}.sh")
        Files.writeString(tmpFile, scriptContent)
        Files.move(tmpFile, scriptPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        scriptPath.toFile().setExecutable(true)
    }

    private fun mergeHooksIntoSettings() {
        val claudeDir = claudeSettingsPath.parent
        Files.createDirectories(claudeDir)

        val existingRoot: JsonObject = if (Files.exists(claudeSettingsPath)) {
            try {
                val content = Files.readString(claudeSettingsPath)
                json.parseToJsonElement(content).jsonObject
            } catch (e: Exception) {
                logger.warn("Could not parse existing settings.json, starting fresh", e)
                JsonObject(emptyMap())
            }
        } else {
            JsonObject(emptyMap())
        }

        val existingHooks = existingRoot["hooks"]?.jsonObject ?: JsonObject(emptyMap())
        val scriptPathStr = scriptPath.toString()

        // New format: each event is an array of matcher groups
        // Each matcher group has optional "matcher" and required "hooks" array
        val hookEvents = listOf("Notification", "Stop", "PostToolUseFailure")

        val mergedHooks = buildJsonObject {
            // Keep existing hooks
            for ((key, value) in existingHooks) {
                if (key !in hookEvents) {
                    put(key, value)
                } else {
                    put(key, mergeMatcherGroups(value.jsonArray, scriptPathStr))
                }
            }
            // Add new hook events that don't exist yet
            for (eventName in hookEvents) {
                if (eventName !in existingHooks) {
                    put(eventName, buildMatcherGroup(scriptPathStr))
                }
            }
        }

        val mergedRoot = buildJsonObject {
            for ((key, value) in existingRoot) {
                if (key != "hooks") put(key, value)
            }
            put("hooks", mergedHooks)
        }

        val tmpSettings = claudeDir.resolve(".tmp-settings-${System.currentTimeMillis()}.json")
        Files.writeString(tmpSettings, json.encodeToString(JsonObject.serializer(), mergedRoot))
        Files.move(tmpSettings, claudeSettingsPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    /**
     * Build a new matcher group array with a single entry containing our hook.
     * Format: [{ "hooks": [{ "type": "command", "command": "..." }] }]
     */
    private fun buildMatcherGroup(scriptPath: String): JsonArray {
        return buildJsonArray {
            add(buildJsonObject {
                put("hooks", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "command")
                        put("command", scriptPath)
                    })
                })
            })
        }
    }

    /**
     * Merge our hook into existing matcher groups.
     * Check if any matcher group already contains our script command.
     * If not, add a new matcher group with our hook.
     */
    private fun mergeMatcherGroups(existing: JsonArray, scriptPath: String): JsonArray {
        val alreadyExists = existing.any { matcherGroup ->
            val hooks = matcherGroup.jsonObject["hooks"]?.jsonArray ?: return@any false
            hooks.any { hook ->
                hook.jsonObject["command"]?.jsonPrimitive?.content == scriptPath
            }
        }
        return if (alreadyExists) {
            existing
        } else {
            buildJsonArray {
                existing.forEach { add(it) }
                add(buildJsonObject {
                    put("hooks", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "command")
                            put("command", scriptPath)
                        })
                    })
                })
            }
        }
    }
}
