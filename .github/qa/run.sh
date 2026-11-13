#!/usr/bin/env bash
# Runs every journey in .github/qa/journeys/ on the booted emulator and collects results in qa-out/.
set -x
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
adb shell settings put global window_animation_scale 0
# Let the launcher and Play services finish their first-boot work
sleep 90
adb shell input keyevent 3
adb install -r -g "$APK"
adb logcat -c
adb logcat -v time > qa-out/logcat.txt 2>&1 &
for j in .github/qa/journeys/${JOURNEYS:-*}.txt; do
  OUT="qa-out/$(basename "$j" .txt)" python3 .github/qa/ui.py "$j"
done
adb logcat -d -b crash > qa-out/crash.txt 2>&1
kill %1 || true
