#!/usr/bin/env bash
set -euo pipefail

# Rebuild the patched upstream server packaged with the Android example.
app_dir="$(cd "$(dirname "$0")/../.." && pwd)"
server_build_dir="${EMMYLUA_BUILD_DIR:-$app_dir/build/emmylua}"
ndk_dir="${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to an installed Android NDK}"
case "$(uname -s)" in
    Darwin) ndk_host=darwin-x86_64 ;;
    Linux) ndk_host=linux-x86_64 ;;
    *) echo "Build on macOS or Linux." >&2; exit 1 ;;
esac
compiler_dir="$ndk_dir/toolchains/llvm/prebuilt/$ndk_host/bin"
mkdir -p "$server_build_dir"
archive="$server_build_dir/emmylua-0.25.1.zip"
curl -fL --retry 2 https://github.com/EmmyLuaLs/emmylua-analyzer-rust/archive/refs/tags/0.25.1.zip -o "$archive"
python3 - "$archive" <<'PY'
import hashlib, sys
with open(sys.argv[1], 'rb') as source:
    assert hashlib.file_digest(source, 'sha256').hexdigest() == '8ff7399120971d99d92f3d95cab599fab717fe44ebb85050891aeb3af3e576d0', 'Source archive checksum mismatch'
PY
unzip -qo "$archive" -d "$server_build_dir"
cd "$server_build_dir/emmylua-analyzer-rust-0.25.1"
git apply "$app_dir/servers/emmylua/semantic-token-length.patch"

for specification in \
    arm64-v8a:aarch64-linux-android:aarch64-linux-android \
    armeabi-v7a:armv7-linux-androideabi:armv7a-linux-androideabi \
    x86:i686-linux-android:i686-linux-android \
    x86_64:x86_64-linux-android:x86_64-linux-android; do
    IFS=: read -r abi target compiler_target <<< "$specification"
    rustup target add "$target"
    target_key="${target//-/_}"
    linker_key="$(printf '%s' "$target_key" | tr '[:lower:]' '[:upper:]')"
    compiler="$compiler_dir/${compiler_target}26-clang"
    env "CARGO_TARGET_${linker_key}_LINKER=$compiler" \
        "CC_${target_key}=$compiler" "AR_${target_key}=$compiler_dir/llvm-ar" \
        CARGO_PROFILE_RELEASE_STRIP=symbols CARGO_PROFILE_RELEASE_OPT_LEVEL=z \
        CARGO_PROFILE_RELEASE_LTO=fat CARGO_PROFILE_RELEASE_CODEGEN_UNITS=1 \
        CARGO_PROFILE_RELEASE_PANIC=abort \
        cargo build --locked --release -p emmylua_ls --target "$target"
    mkdir -p "$app_dir/src/main/jniLibs/$abi"
    cp "target/$target/release/emmylua_ls" "$app_dir/src/main/jniLibs/$abi/libemmylua_ls.so"
done
