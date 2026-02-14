# Claude Code Notifications

IntelliJ/WebStorm plugin that delivers IDE balloon and macOS native notifications for [Claude Code](https://docs.anthropic.com/en/docs/claude-code) CLI events.

Never miss when Claude asks a question, finishes a task, or hits an error — even when your IDE is in the background.

## How It Works

```
Claude Code → Hook Script → Event JSON → Plugin (WatchService) → Notification
```

1. Claude Code [hooks](https://code.claude.com/docs/en/hooks) trigger a shell script on each event
2. The script writes a JSON file to `~/.claude-code-notifications/`
3. The plugin watches this directory via Java NIO WatchService
4. New events are parsed, displayed as notifications, and cleaned up

## Notifications

| Event | When | Notification |
|---|---|---|
| **Question** | Claude asks a question or requests permission | IDE balloon + system notification |
| **Completion** | Claude finishes a task | IDE balloon + system notification |
| **Error** | A tool fails (build error, command failure, etc.) | Sticky IDE balloon + system notification |

## Installation

### From JetBrains Marketplace

1. Open **Settings → Plugins → Marketplace**
2. Search for **"Claude Code Notifications"**
3. Click **Install** and restart your IDE

### From ZIP

1. Download the latest release from [Releases](https://github.com/ahmetrukenboyaci/claude-code-notifications/releases)
2. **Settings → Plugins → ⚙️ → Install Plugin from Disk**
3. Select the ZIP file and restart

### Hook Setup

The plugin auto-installs hooks on first startup. You can also manually install them:

**Settings → Tools → Claude Code Notifications → Install / Update Hooks**

Or via menu: **Tools → Install Claude Code Hooks**

This adds the following to your `~/.claude/settings.json`:

```json
{
  "hooks": {
    "Notification": [{ "hooks": [{ "type": "command", "command": "~/.claude-code-notifications/claude-code-notify.sh" }] }],
    "Stop": [{ "hooks": [{ "type": "command", "command": "~/.claude-code-notifications/claude-code-notify.sh" }] }],
    "PostToolUseFailure": [{ "hooks": [{ "type": "command", "command": "~/.claude-code-notifications/claude-code-notify.sh" }] }]
  }
}
```

> **Note:** Hooks are loaded at session start. Restart your Claude Code session after installing hooks.

## Settings

**Settings → Tools → Claude Code Notifications**

- Enable/disable all notifications
- Toggle IDE balloons on/off
- Toggle macOS system notifications on/off
- Toggle notification sounds on/off
- Per-event toggles: questions, completions, errors
- Hook installation status indicator

## Testing

Send test notifications to verify everything works:

```bash
# Send all three event types
./test-notifications.sh all

# Or individually
./test-notifications.sh question
./test-notifications.sh completion
./test-notifications.sh error
```

## Building from Source

```bash
# Requires JDK 21
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

./gradlew buildPlugin
# Output: build/distributions/claude-code-notifications-plugin-1.0.0.zip

./gradlew runIde
# Launches a sandbox IDE for testing
```

## Requirements

- IntelliJ-based IDE 2024.3+ (WebStorm, IntelliJ IDEA, PyCharm, etc.)
- macOS (for system notifications via osascript)
- Claude Code CLI with hooks support
- Python 3 (used by hook script for JSON parsing)

## Architecture

| Component | Role |
|---|---|
| `claude-code-notify.sh` | Hook script — reads stdin JSON, writes atomic event files |
| `EventFileWatcherService` | NIO WatchService loop, atomic claim via rename |
| `NotificationDispatcher` | Routes events to IDE balloons with "Focus Terminal" action |
| `SystemNotificationService` | macOS `osascript` notifications with sound |
| `HookInstallerService` | Non-destructive merge into `~/.claude/settings.json` |

Multi-IDE safe: when multiple IDE instances run, the first to rename an event file wins.

## License

[MIT](LICENSE)
