#!/usr/bin/env bash
# Claude Code Notification Hook Script
# Reads event data from stdin (JSON), writes an event file for the IntelliJ plugin.

set -euo pipefail

NOTIF_DIR="$HOME/.claude-code-notifications"
mkdir -p "$NOTIF_DIR"

# Read stdin (Claude Code passes hook data as JSON via stdin)
INPUT=$(cat)

if [ -z "$INPUT" ]; then
    exit 0
fi

# Parse all fields from input JSON in a single python3 call
eval "$(echo "$INPUT" | python3 -c "
import sys, json, shlex
try:
    data = json.load(sys.stdin)
    hook_event = data.get('hook_event_name', '')
    session_id = data.get('session_id', '')
    cwd = data.get('cwd', '')
    message = data.get('message', '')
    title = data.get('title', '')
    notification_type = data.get('notification_type', '')
    tool_name = data.get('tool_name', 'Tool')
    error = data.get('error', '')
    stop_hook_active = data.get('stop_hook_active', False)

    print(f'HOOK_EVENT={shlex.quote(hook_event)}')
    print(f'SESSION_ID={shlex.quote(session_id)}')
    print(f'CWD={shlex.quote(cwd)}')
    print(f'INPUT_MESSAGE={shlex.quote(message)}')
    print(f'INPUT_TITLE={shlex.quote(title)}')
    print(f'NOTIFICATION_TYPE={shlex.quote(notification_type)}')
    print(f'TOOL_NAME={shlex.quote(tool_name)}')
    print(f'ERROR_MSG={shlex.quote(error[:200] if error else \"\")}')
    print(f'STOP_HOOK_ACTIVE={shlex.quote(str(stop_hook_active).lower())}')
except Exception as e:
    print(f'HOOK_EVENT=unknown')
    print(f'SESSION_ID=')
    print(f'CWD=')
    print(f'INPUT_MESSAGE=')
    print(f'INPUT_TITLE=')
    print(f'NOTIFICATION_TYPE=')
    print(f'TOOL_NAME=Tool')
    print(f'ERROR_MSG=')
    print(f'STOP_HOOK_ACTIVE=false')
" 2>/dev/null)"

# Get millisecond timestamp (macOS doesn't support date +%s%N)
if command -v python3 &>/dev/null; then
    TIMESTAMP=$(python3 -c "import time; print(int(time.time() * 1000))")
else
    TIMESTAMP=$(($(date +%s) * 1000))
fi
RANDOM_SUFFIX=$RANDOM

# Determine event type, title, message based on hook event
EVENT_TYPE="completion"
SEVERITY="info"
TITLE=""
MESSAGE=""

case "$HOOK_EVENT" in
    Notification)
        EVENT_TYPE="question"
        SEVERITY="info"
        TITLE="${INPUT_TITLE:-Claude needs your attention}"
        MESSAGE="${INPUT_MESSAGE:-Claude is waiting for your input}"
        ;;
    Stop)
        EVENT_TYPE="completion"
        SEVERITY="info"
        TITLE="Claude has finished"
        MESSAGE="Task completed"
        ;;
    PostToolUseFailure)
        EVENT_TYPE="error"
        SEVERITY="error"
        TITLE="Tool error: $TOOL_NAME"
        MESSAGE="${ERROR_MSG:-Tool execution failed}"
        ;;
    *)
        # Unknown hook event, skip
        exit 0
        ;;
esac

# Build JSON event file safely with python3
EVENT_JSON=$(python3 -c "
import json, sys
event = {
    'type': sys.argv[1],
    'severity': sys.argv[2],
    'title': sys.argv[3],
    'message': sys.argv[4],
    'cwd': sys.argv[5],
    'timestamp': int(sys.argv[6]),
    'hookEvent': sys.argv[7],
    'sessionId': sys.argv[8]
}
print(json.dumps(event))
" "$EVENT_TYPE" "$SEVERITY" "$TITLE" "$MESSAGE" "$CWD" "$TIMESTAMP" "$HOOK_EVENT" "$SESSION_ID" 2>/dev/null)

if [ -z "$EVENT_JSON" ]; then
    exit 0
fi

# Atomic write: write to temp file, then move
FILENAME="event-${TIMESTAMP}-${RANDOM_SUFFIX}.json"
TMP_FILE="$NOTIF_DIR/.tmp-${FILENAME}"
FINAL_FILE="$NOTIF_DIR/${FILENAME}"

echo "$EVENT_JSON" > "$TMP_FILE"
mv "$TMP_FILE" "$FINAL_FILE"
