#!/usr/bin/env bash
# Runs the OpenHubX end-to-end tests on a connected phone or emulator.
#
# The tests install a separate app (com.darktile.openhubx.e2e), so your real app and its
# data are never touched. Gradle uninstalls the e2e app when the run ends.
#
# Signed-in tests need a GitHub token. Put it in GH_TOKEN; without it they are skipped:
#   GH_TOKEN=github_pat_... tools/e2e/run-e2e.sh
# Run only one test class:
#   tools/e2e/run-e2e.sh GuestModeE2eTest
set -euo pipefail
cd "$(dirname "$0")/../.."

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export JAVA_HOME="${JAVA_HOME:-$HOME/jdks/amazon-corretto-25.0.4.8.1-linux-x64}"
adb="$ANDROID_HOME/platform-tools/adb"

if ! "$adb" get-state >/dev/null 2>&1; then
  echo "No device found. Plug in a phone with USB debugging on, or start an emulator." >&2
  exit 1
fi
echo "Device: $("$adb" shell getprop ro.product.model) (Android $("$adb" shell getprop ro.build.version.release))"

args=(connectedNormalE2eAndroidTest -Pe2e --no-daemon --no-watch-fs -Dorg.gradle.vfs.watch=false)
if [[ -n "${GH_TOKEN:-}" ]]; then
  args+=("-Pandroid.testInstrumentationRunnerArguments.ghToken=$GH_TOKEN")
else
  echo "GH_TOKEN not set: signed-in tests (AccountE2eTest) will be skipped."
fi
if [[ -n "${1:-}" ]]; then
  args+=("-Pandroid.testInstrumentationRunnerArguments.class=com.darktile.openhubx.e2e.$1")
fi

./gradlew "${args[@]}"
echo "Report: app/build/reports/androidTests/connected/e2e/flavors/normal/index.html"
