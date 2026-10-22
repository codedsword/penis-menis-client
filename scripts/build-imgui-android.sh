#!/bin/bash
# Builds imgui-java's native library for Android arm64 (MojoLauncher, PojavLauncher, ...).
# imgui-java only publishes x86_64 linux natives, which cannot be loaded there.
#
# usage: ANDROID_NDK=/path/to/android-ndk-r27c scripts/build-imgui-android.sh
# output: src/main/resources/natives/android-arm64/libimgui-java64.so
set -euo pipefail

IMGUI_VERSION=v1.90.0
API=24
ROOT=$(cd "$(dirname "$0")/.." && pwd)
WORK=${WORK:-$(mktemp -d)}
TC="$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT="$ROOT/src/main/resources/natives/android-arm64"

echo "Working in $WORK"
cd "$WORK"
git clone -q --depth 1 --branch "$IMGUI_VERSION" --recurse-submodules --shallow-submodules https://github.com/SpaiR/imgui-java.git
cd imgui-java

# Generate the JNI sources. The task then tries to compile them for linux with ant, that part is
# not needed here and fails without ant, so only the generated jni directory is checked.
./gradlew imgui-binding:generateLibs -Denvs=linux -Dfreetype=true -Dlocal --no-daemon || true
JNI=imgui-binding/build/jni
test -f "$JNI/imgui.cpp" || { echo "JNI sources were not generated"; exit 1; }

# FreeType, static
mkdir -p "$WORK/freetype"
tar -xzf vendor/freetype-2.13.3.tar.gz -C "$WORK/freetype" --strip-components=1
(cd "$WORK/freetype" && ./configure --host=aarch64-linux-android \
    CC="$TC/aarch64-linux-android$API-clang" AR="$TC/llvm-ar" RANLIB="$TC/llvm-ranlib" CFLAGS="-fPIC -O2" \
    --enable-static --disable-shared --without-zlib --without-bzip2 --without-png --without-harfbuzz --without-brotli \
    && make -j"$(nproc)")

# imgui + binding. memcpy_wrap.c is a glibc versioning workaround and is skipped.
mkdir -p "$WORK/obj"
cd "$JNI"
find . -name '*.cpp' | while read -r src; do
    obj="$WORK/obj/$(echo "${src#./}" | tr '/' '_').o"
    "$TC/aarch64-linux-android$API-clang++" -c -O2 -fPIC -w -I"$WORK/freetype/include" \
        -Ijni-headers -Ijni-headers/linux -I. "$src" -o "$obj" &
done
wait

# 16 KB pages are required on Android 15+ devices
"$TC/aarch64-linux-android$API-clang++" -shared -o "$WORK/libimgui-java64.so" "$WORK"/obj/*.o \
    -L"$WORK/freetype/objs/.libs" -lfreetype -static-libstdc++ -llog -lm \
    -Wl,-z,max-page-size=16384 -Wl,--no-undefined
"$TC/llvm-strip" --strip-unneeded "$WORK/libimgui-java64.so"

mkdir -p "$OUT"
cp "$WORK/libimgui-java64.so" "$OUT/"
echo "Wrote $OUT/libimgui-java64.so"
