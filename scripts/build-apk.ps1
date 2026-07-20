# Builds the sideloadable Pangea APK (fdroid flavor, release build type).
# Signing: uses the release keystore if keystore.properties provides one;
# otherwise androidApp/build.gradle.kts falls back to the debug keystore
# (CN=Android Debug), which is fine for direct sideloading.
# ABI splits are enabled; the universal APK works on any device/emulator.
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
.\gradlew.bat :androidApp:assembleFdroidRelease
$apk = Get-ChildItem "androidApp\build\outputs\apk\fdroid\release\*universal*.apk" | Select-Object -First 1
if ($null -eq $apk) { $apk = Get-ChildItem "androidApp\build\outputs\apk\fdroid\release\*.apk" | Select-Object -First 1 }
if ($null -eq $apk) { throw "No APK found under androidApp\build\outputs\apk\fdroid\release" }
Write-Host ""
Write-Host "APK ready: $($apk.FullName)"
Write-Host "Sideload:  adb install -r `"$($apk.FullName)`""
