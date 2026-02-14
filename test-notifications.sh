#!/usr/bin/env bash
# Manual test script for Claude Code Notifications plugin
# Usage: ./test-notifications.sh [question|completion|error|all]

set -euo pipefail

NOTIF_DIR="$HOME/.claude-code-notifications"
mkdir -p "$NOTIF_DIR"

HOOK_SCRIPT="$(dirname "$0")/src/main/resources/hooks/claude-code-notify.sh"

send_question() {
    echo "-> Sending question event..."
    echo '{"hook_event_name":"Notification","session_id":"manual-test","cwd":"'"$PWD"'","message":"Do you want me to refactor the authentication module?","title":"Claude needs your attention","notification_type":"idle_prompt"}' \
    | bash "$HOOK_SCRIPT"
    echo "  OK Question event sent"
}

send_completion() {
    echo "-> Sending completion event..."
    echo '{"hook_event_name":"Stop","session_id":"manual-test","cwd":"'"$PWD"'","stop_hook_active":false}' \
    | bash "$HOOK_SCRIPT"
    echo "  OK Completion event sent"
}

send_error() {
    echo "-> Sending error event..."
    echo '{"hook_event_name":"PostToolUseFailure","session_id":"manual-test","cwd":"'"$PWD"'","tool_name":"Bash","error":"npm ERR! code ELIFECYCLE - build failed with exit code 1"}' \
    | bash "$HOOK_SCRIPT"
    echo "  OK Error event sent"
}

case "${1:-all}" in
    question)   send_question ;;
    completion) send_completion ;;
    error)      send_error ;;
    all)
        send_question
        sleep 1
        send_completion
        sleep 1
        send_error
        ;;
    *)
        echo "Usage: $0 [question|completion|error|all]"
        exit 1
        ;;
esac

echo ""
echo "Event files in $NOTIF_DIR:"
ls -lt "$NOTIF_DIR"/event-*.json 2>/dev/null | head -5 || echo "  (none - plugin already consumed them)"
