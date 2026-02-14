plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.claudecode.notifications"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.claudecode.notifications"
        name = "Claude Code Notifications"
        version = "1.0.0"
        description = """
            <p>Never miss when Claude asks a question, finishes a task, or hits an error — even when your IDE is in the background.</p>

            <h2>How It Works</h2>
            <p><code>Claude Code → Hook Script → Event JSON → Plugin (WatchService) → Notification</code></p>
            <p>Uses Claude Code's <a href="https://code.claude.com/docs/en/hooks">hook system</a> to capture events and deliver them as IDE balloon notifications and macOS native notifications.</p>

            <h2>Notifications</h2>
            <ul>
                <li><b>Question</b> — Claude asks a question or requests permission → IDE balloon + system notification</li>
                <li><b>Completion</b> — Claude finishes a task → IDE balloon + system notification</li>
                <li><b>Error</b> — A tool fails (build error, command failure, etc.) → Sticky balloon + system notification</li>
            </ul>

            <h2>Features</h2>
            <ul>
                <li>Automatic hook installation on first startup</li>
                <li>macOS native notifications with sound</li>
                <li>"Focus Terminal" action button on question notifications</li>
                <li>Multi-IDE safe — atomic file claim prevents duplicate notifications</li>
                <li>Per-event toggles: questions, completions, errors</li>
                <li>Catches events received while IDE was closed</li>
            </ul>

            <h2>Setup</h2>
            <p>Hooks are auto-installed on IDE startup. You can also install manually via <b>Tools → Install Claude Code Hooks</b>.</p>
            <p>Configure in <b>Settings → Tools → Claude Code Notifications</b>.</p>

            <h2>Requirements</h2>
            <ul>
                <li>macOS (for system notifications)</li>
                <li><a href="https://docs.anthropic.com/en/docs/claude-code">Claude Code CLI</a> with hooks support</li>
                <li>Python 3 (used by hook script)</li>
            </ul>
        """.trimIndent()
        vendor {
            name = "Claude Code Community"
        }
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}
