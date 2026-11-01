$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
if(!$env:JAVA_HOME){$env:JAVA_HOME="$PSScriptRoot\tools\jdk-21.0.9+10"}
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$sdk=if($env:ANDROID_HOME){$env:ANDROID_HOME}else{"$PSScriptRoot\tools\android-sdk"}
$bt="$sdk\build-tools\35.0.0"
$ndk="$sdk\ndk\28.2.13676358\toolchains\llvm\prebuilt\windows-x86_64\bin"
$ndi="$PSScriptRoot\tools\NDI SDK for Android"
function Check { if($LASTEXITCODE -ne 0){throw "Команда завершилась с кодом $LASTEXITCODE"} }
New-Item -ItemType Directory -Force build\generated,build\classes,build\dex,build\package\lib,build\package\assets,dist | Out-Null
foreach($item in @(@('arm64-v8a','aarch64-linux-android29'),@('armeabi-v7a','armv7a-linux-androideabi29'),@('x86_64','x86_64-linux-android29'))){
 $abi=$item[0];$target=$item[1]
 New-Item -ItemType Directory -Force "build\package\lib\$abi" | Out-Null
 & "$ndk\clang++.exe" "--target=$target" -shared -fPIC -O2 -std=c++17 -static-libstdc++ '-Wl,-z,max-page-size=16384' '-Wl,-soname,libndicast.so' "-I$ndi\include" native\ndicast.cpp "-L$ndi\lib\$abi" -lndi -o "build\package\lib\$abi\libndicast.so"
 Check
 Copy-Item "$ndi\lib\$abi\libndi.so" "build\package\lib\$abi\libndi.so"
}
Copy-Item "$ndi\NDI SDK License Agreement.txt" build\package\assets\NDI-License.txt
Copy-Item "$ndi\licenses\*" build\package\assets
& "$bt\aapt2.exe" compile --dir res -o build\resources.zip
Check
& "$bt\aapt2.exe" link -o build\unsigned.apk -I "$sdk\platforms\android-35\android.jar" --manifest AndroidManifest.xml --java build\generated build\resources.zip
Check
& "$env:JAVA_HOME\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -bootclasspath "$sdk\platforms\android-35\android.jar;$bt\core-lambda-stubs.jar" -d build\classes (Get-ChildItem src,build\generated -Filter *.java -Recurse).FullName
Check
& "$env:JAVA_HOME\bin\jar.exe" cf build\classes.jar -C build\classes .
Check
& "$bt\d8.bat" --lib "$sdk\platforms\android-35\android.jar" --min-api 29 --output build\dex build\classes.jar
Check
Copy-Item build\dex\classes.dex build\package\classes.dex
& "$env:JAVA_HOME\bin\jar.exe" uf build\unsigned.apk -C build\package .
Check
& "$bt\zipalign.exe" -f -P 16 4 build\unsigned.apk build\aligned.apk
Check
if(Test-Path signing\local.ps1){. "$PSScriptRoot\signing\local.ps1"}
if(!$env:NDI_SIGNING_PASSWORD){throw "Задайте NDI_SIGNING_PASSWORD или signing/local.ps1"}
if(!(Test-Path signing\release.jks)){
 New-Item -ItemType Directory -Force signing | Out-Null
 & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -keystore signing\release.jks -storepass $env:NDI_SIGNING_PASSWORD -keypass $env:NDI_SIGNING_PASSWORD -alias ndicast -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=NDI ScreenCast, OU=Personal, O=iLeech, C=RU'
 Check
}
& "$bt\apksigner.bat" sign --ks signing\release.jks --ks-key-alias ndicast --ks-pass env:NDI_SIGNING_PASSWORD --out dist\NDIScreenCast-1.4.apk build\aligned.apk
Check
& "$bt\apksigner.bat" verify --verbose dist\NDIScreenCast-1.4.apk
Check
& "$bt\zipalign.exe" -c -P 16 4 dist\NDIScreenCast-1.4.apk
Check
Get-FileHash dist\NDIScreenCast-1.4.apk -Algorithm SHA256 | Format-List
