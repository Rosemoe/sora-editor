# EmmyLua example server

The example bundles [EmmyLua Language Server 0.25.1](https://github.com/EmmyLuaLs/emmylua-analyzer-rust/releases/tag/0.25.1)
for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` (Android API 26+).
The executables are `app/src/main/jniLibs/<abi>/libemmylua_ls.so`. The `.so` name allows Android
packaging to extract them to `nativeLibraryDir`; they are standalone PIE executables, not JNI libraries.

`LspLanguageServerService` starts a process for each local socket connection and forwards standard
LSP messages between the socket and the process's stdin/stdout. No server download is needed at runtime.
The server advertises full semantic tokens and full document synchronization. Both Java and Kotlin
example activities configure `TextMateSemanticTokenStyleProvider` explicitly.

The server embeds `crates/emmylua_code_analysis/resources/std` using Rust's `include_dir!` macro.
It extracts these standard-library definitions to the app's `cacheDir/emmylua/std`; the old
`testProject/std/Lua53` copy is therefore unnecessary. Process stderr is in `cacheDir/emmylua/stderr.log`.

The bundled build applies `semantic-token-length.patch`: upstream computes multiline token lengths
as end-column minus start-column, losing intervening lines. The patch counts UTF-16 units across
the actual source range, including line separators and supplementary characters, and converts
token columns to UTF-16 as well.

To rebuild, install Git, Rust, Python 3.11+, and Android NDK 29, then run:

```sh
ANDROID_NDK_HOME=/path/to/android-ndk app/servers/emmylua/build.sh
```

On Windows, install Git, Rust and Android NDK 29, then run from `cmd.exe`:

```bat
set "ANDROID_NDK_HOME=C:\path\to\android-ndk"
app\servers\emmylua\build.bat
```

The batch script uses PowerShell to download, verify and extract the same source archive.

The bundled files were built with Rust 1.97.1 and NDK 29.0.14206865, using the upstream lockfile,
`opt-level=z`, full LTO, one codegen unit, `panic=abort`, and stripped symbols. `build.sh` verifies the upstream source ZIP's SHA-256.
The server LICENSE is packaged under `assets/licenses/emmylua`.

The bundled size-optimized executables are approximately 6.2 MiB (arm64-v8a), 4.7 MiB
(armeabi-v7a), 7.4 MiB (x86), and 7.5 MiB (x86_64) before APK compression.
