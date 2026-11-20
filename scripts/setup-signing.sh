#!/bin/bash
# Make in-app updates (OTA) work: create the release key once and store it in this repository's
# GitHub Actions secrets, so every build is signed with the same key and installs over the last
# one. Without it, CI signs with a throwaway key and the bell's update can't install.
# Usage: scripts/setup-signing.sh
#        KEY=/path/to/tvhome-release.jks scripts/setup-signing.sh   use a key you already have
# Needs the JDK's keytool (docs/install-*.md, step 2) and the GitHub CLI signed in (`gh auth login`).
set -euo pipefail
cd "$(dirname "$0")/.."
KEY="${KEY:-tvhome-release.jks}"
ALIAS="${ALIAS:-tvhome}"
die() { echo "Error: $*" >&2; exit 1; }

command -v keytool >/dev/null || die "keytool not found: install a JDK 17-21 (docs/install-macos.md, docs/install-linux.md or docs/install-windows.md)."
command -v gh >/dev/null || die "gh not found: install the GitHub CLI (https://cli.github.com), then run: gh auth login"
gh auth status >/dev/null 2>&1 || die "not signed in to GitHub. Run: gh auth login"

if [ -f "$KEY" ]; then
  echo "Using the release key already at $KEY."
else
  echo "Creating $KEY. Choose a password and keep it in your password manager."
  keytool -genkeypair -v -keystore "$KEY" -alias "$ALIAS" \
    -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=TV Home"
fi

printf 'Key password: '
read -rs PASSWORD
echo
[ -n "$PASSWORD" ] || die "no password given"

base64 < "$KEY" | tr -d '\n' | gh secret set SIGNING_KEYSTORE_BASE64
printf '%s' "$PASSWORD" | gh secret set SIGNING_STORE_PASSWORD
printf '%s' "$PASSWORD" | gh secret set SIGNING_KEY_PASSWORD
gh secret set SIGNING_KEY_ALIAS --body "$ALIAS"

echo
echo "Done: the four secrets are set, so CI now signs with this key."
echo "Back up $KEY and its password somewhere private, outside this repository: without them no"
echo "future build can update the installed app."
echo "Next: re-run the Build workflow (Actions -> Build -> Re-run all jobs) and install that APK on"
echo "the TV once by hand; updates install from the bell after that."
