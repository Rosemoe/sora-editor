@echo off
setlocal EnableExtensions

rem Rebuild the patched upstream server packaged with the Android example.
if not defined ANDROID_NDK_HOME (
    echo Set ANDROID_NDK_HOME to an installed Android NDK. 1>&2
    exit /b 1
)
for %%I in ("%~dp0..\..") do set "APP_DIR=%%~fI"
if not defined EMMYLUA_BUILD_DIR set "EMMYLUA_BUILD_DIR=%APP_DIR%\build\emmylua"
set "COMPILER_DIR=%ANDROID_NDK_HOME%\toolchains\llvm\prebuilt\windows-x86_64\bin"
if not exist "%COMPILER_DIR%\llvm-ar.exe" (
    echo Android NDK Windows toolchain not found. 1>&2
    exit /b 1
)
if not exist "%EMMYLUA_BUILD_DIR%" mkdir "%EMMYLUA_BUILD_DIR%"
set "EMMYLUA_ARCHIVE=%EMMYLUA_BUILD_DIR%\emmylua-0.25.1.zip"
powershell -NoProfile -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri 'https://github.com/EmmyLuaLs/emmylua-analyzer-rust/archive/refs/tags/0.25.1.zip' -OutFile $env:EMMYLUA_ARCHIVE; if ((Get-FileHash -Algorithm SHA256 -LiteralPath $env:EMMYLUA_ARCHIVE).Hash -ne '8ff7399120971d99d92f3d95cab599fab717fe44ebb85050891aeb3af3e576d0') { throw 'Source archive checksum mismatch' }; Expand-Archive -LiteralPath $env:EMMYLUA_ARCHIVE -DestinationPath $env:EMMYLUA_BUILD_DIR -Force"
if errorlevel 1 exit /b 1
pushd "%EMMYLUA_BUILD_DIR%\emmylua-analyzer-rust-0.25.1" || exit /b 1

git apply "%~dp0semantic-token-length.patch"
if errorlevel 1 goto failed

call :build arm64-v8a aarch64-linux-android aarch64-linux-android
if errorlevel 1 goto failed
call :build armeabi-v7a armv7-linux-androideabi armv7a-linux-androideabi
if errorlevel 1 goto failed
call :build x86 i686-linux-android i686-linux-android
if errorlevel 1 goto failed
call :build x86_64 x86_64-linux-android x86_64-linux-android
if errorlevel 1 goto failed
popd
exit /b 0

:build
setlocal
set "ABI=%~1"
set "TARGET=%~2"
set "TARGET_KEY=%TARGET:-=_%"
set "COMPILER_TARGET=%~3"
set "COMPILER=%COMPILER_DIR%\%COMPILER_TARGET%26-clang.cmd"
rem Windows environment variable names are case-insensitive.
set "CARGO_TARGET_%TARGET_KEY%_LINKER=%COMPILER%"
set "CC_%TARGET_KEY%=%COMPILER%"
set "AR_%TARGET_KEY%=%COMPILER_DIR%\llvm-ar.exe"
set "CARGO_PROFILE_RELEASE_STRIP=symbols"
set "CARGO_PROFILE_RELEASE_OPT_LEVEL=z"
set "CARGO_PROFILE_RELEASE_LTO=fat"
set "CARGO_PROFILE_RELEASE_CODEGEN_UNITS=1"
set "CARGO_PROFILE_RELEASE_PANIC=abort"
rustup target add %TARGET%
if errorlevel 1 exit /b 1
cargo build --locked --release -p emmylua_ls --target %TARGET%
if errorlevel 1 exit /b 1
if not exist "%APP_DIR%\src\main\jniLibs\%ABI%" mkdir "%APP_DIR%\src\main\jniLibs\%ABI%"
copy /Y "target\%TARGET%\release\emmylua_ls" "%APP_DIR%\src\main\jniLibs\%ABI%\libemmylua_ls.so" >nul
if errorlevel 1 exit /b 1
endlocal
exit /b 0

:failed
popd
exit /b 1
