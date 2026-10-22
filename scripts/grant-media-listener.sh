#!/bin/sh
# Grants the notification-listener role needed by NowPlayingCard.
set -eu

ADB="${ADB:-adb}"
SERIAL="${ANDROID_SERIAL:-}"
DRY_RUN=false
COMPONENT="com.joaofsantos.tcllauncher/.MediaSessionListenerService"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --dry-run) DRY_RUN=true ;;
    --serial) SERIAL="$2"; shift ;;
    -h|--help) echo "Usage: $0 [--dry-run] [--serial SERIAL]"; exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
  shift
done

[ -n "$SERIAL" ] || { echo "Defina ANDROID_SERIAL=<ip-da-tv>:5555 ou use --serial" >&2; exit 2; }

if "$DRY_RUN"; then
  echo "+ $ADB -s $SERIAL shell cmd notification allow_listener $COMPONENT"
else
  "$ADB" -s "$SERIAL" shell cmd notification allow_listener "$COMPONENT"
fi
