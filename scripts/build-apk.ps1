# Builds the sideloadable Pangea APK (fdroid flavor, release build type) and archives
# a dated, version-stamped copy into <project>\released_apks\.
#
# Signing: uses the release keystore if keystore.properties provides one; otherwise
# androidApp/build.gradle.kts falls back to the debug keystore (CN=Android Debug),
# which is fine for direct sideloading.
# ABI splits are enabled; the universal APK works on any device/emulator.
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

.\gradlew.bat :androidApp:assembleFdroidRelease

$apk = Get-ChildItem "androidApp\build\outputs\apk\fdroid\release\*universal*.apk" | Select-Object -First 1
if ($null -eq $apk) { $apk = Get-ChildItem "androidApp\build\outputs\apk\fdroid\release\*.apk" | Select-Object -First 1 }
if ($null -eq $apk) { throw "No APK found under androidApp\build\outputs\apk\fdroid\release" }

# Stamp the archive copy with the APK's own version + today's date.
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$aapt = Get-ChildItem (Join-Path $sdk "build-tools\*\aapt2.exe") -ErrorAction SilentlyContinue |
    Sort-Object FullName | Select-Object -Last 1
$version = "unknown"
$code = "0"
if ($aapt) {
    $badging = (& $aapt.FullName dump badging $apk.FullName 2>$null) -join "`n"
    if ($badging -match "versionName='([^' ]+)") { $version = $Matches[1] }
    if ($badging -match "versionCode='([^']+)'") { $code = $Matches[1] }
}

$archiveDir = Join-Path $PWD "..\released_apks"
New-Item -ItemType Directory -Force -Path $archiveDir | Out-Null
$archiveDir = (Resolve-Path $archiveDir).Path
$stamp = Get-Date -Format 'yyyy-MM-dd'
$archive = Join-Path $archiveDir "Pangea-$version-vc$code-fdroid-release-$stamp.apk"
Copy-Item $apk.FullName $archive -Force

Write-Host ""
Write-Host "APK ready: $($apk.FullName)"
Write-Host "Archived:  $archive"
Write-Host "Package:   com.geeksville.mesh.pangea  (distinct from upstream Meshtastic's"
Write-Host "           com.geeksville.mesh, so both apps can be installed side by side)"
Write-Host "Sideload:  adb install -r `"$($apk.FullName)`""
