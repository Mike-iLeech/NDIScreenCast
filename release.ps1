$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
[xml]$manifest=Get-Content AndroidManifest.xml
$version=$manifest.manifest.GetAttribute('versionName','http://schemas.android.com/apk/res/android')
$code=[long]$manifest.manifest.GetAttribute('versionCode','http://schemas.android.com/apk/res/android')
$filename="NDIScreenCast-$version.apk"
$apk=Join-Path $PSScriptRoot "dist/$filename"
if(!(Test-Path $apk)){throw 'Сначала соберите APK'}
$update=@{versionCode=$code;versionName=$version;apkUrl="https://github.com/Mike-iLeech/NDIScreenCast/releases/download/v$version/$filename";sha256=(Get-FileHash $apk -Algorithm SHA256).Hash.ToLowerInvariant()}
$update | ConvertTo-Json | Set-Content -Encoding utf8NoBOM dist/update.json
Write-Host "Готовы $filename и update.json. Публикуйте их одним проверенным GitHub Release v$version."
