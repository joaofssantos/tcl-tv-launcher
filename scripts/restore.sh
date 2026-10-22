#!/bin/sh
# Restore packages deliberately disabled by this project and return HOME to TCL's
# original Google TV launcher. This script is deliberately conservative: it
# never changes the original packages-disabled baseline and removes only the
# launcher package named explicitly with --launcher-package.
set -eu

ADB="${ADB:-adb}"
SERIAL="${ANDROID_SERIAL:-}"
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DRY_RUN=false
LAUNCHER_PACKAGE=""
ORIGINAL_HOME="com.google.android.tvlauncher/.MainActivity"

usage() {
  echo "Usage: $0 [--dry-run] --launcher-package PACKAGE [--serial SERIAL]" >&2
  exit 2
}

run() {
  if "$DRY_RUN"; then
    printf '+ '
    printf '%s ' "$@"
    printf '\n'
  else
    "$@"
  fi
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --dry-run) DRY_RUN=true ;;
    --launcher-package)
      [ "$#" -ge 2 ] || usage
      LAUNCHER_PACKAGE="$2"
      shift
      ;;
    --serial)
      [ "$#" -ge 2 ] || usage
      SERIAL="$2"
      shift
      ;;
    -h|--help) usage ;;
    *) usage ;;
  esac
  shift
done

[ -n "$SERIAL" ] || { echo "Defina ANDROID_SERIAL=<ip-da-tv>:5555 ou use --serial" >&2; exit 2; }

[ -n "$LAUNCHER_PACKAGE" ] || usage
case "$LAUNCHER_PACKAGE" in
  *[!A-Za-z0-9._]*|""|.*|*.)
    echo "Invalid launcher package: $LAUNCHER_PACKAGE" >&2
    exit 2
    ;;
esac

adb_cmd() { "$ADB" -s "$SERIAL" "$@"; }

enable_from_file() {
  file="$1"
  [ -f "$file" ] || return 0
  while IFS= read -r package || [ -n "$package" ]; do
    [ -z "$package" ] && continue
    case "$package" in \#*) continue ;; esac
    package=${package#package:}
    run adb_cmd shell pm enable --user 0 "$package"
  done < "$file"
}

# packages-disabled.txt is the original baseline and must remain untouched.
# debloat-applied.txt records only owner-approved disables made by this project.
enable_from_file "$ROOT_DIR/backup/debloat-applied.txt"

run adb_cmd shell cmd package set-home-activity "$ORIGINAL_HOME"
run adb_cmd shell pm uninstall --user 0 "$LAUNCHER_PACKAGE"

echo "Restore complete. Reboot the TV if the HOME key does not update immediately."
