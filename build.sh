#!/usr/bin/env bash
# Manual build for Dotpray (no Gradle needed).
#
#   SDK=/opt/android-sdk JAVA=/opt/jdk ./build.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK="${SDK:-/opt/android-sdk}"
JAVA_HOME="${JAVA:-/opt/jdk}"
BUILD_TOOLS="$SDK/build-tools/37.0.0"
ANDROID_JAR="$SDK/platforms/android-37.0/android.jar"

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

AAPT2="$BUILD_TOOLS/aapt2"
D8="$BUILD_TOOLS/lib/d8.jar"
APKSIGNER="$BUILD_TOOLS/lib/apksigner.jar"
ZIPALIGN="$BUILD_TOOLS/zipalign"

MIN_SDK=26
TARGET_SDK=35
VERSION_CODE=9
VERSION_NAME="1.5"

OUT="$ROOT/build"
APK_NAME="dotpray-${VERSION_NAME}.apk"

rm -rf "$OUT"
mkdir -p "$OUT/compiled" "$OUT/gen" "$OUT/classes" "$OUT/dex" "$OUT/assets"

echo "▸ compiling resources"
"$AAPT2" compile --dir "$ROOT/app/src/main/res" -o "$OUT/compiled/resources.zip"

echo "▸ linking resources"
"$AAPT2" link \
    -o "$OUT/base.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$ROOT/app/src/main/AndroidManifest.xml" \
    -R "$OUT/compiled/resources.zip" \
    --java "$OUT/gen" \
    --min-sdk-version "$MIN_SDK" \
    --target-sdk-version "$TARGET_SDK" \
    --version-code "$VERSION_CODE" \
    --version-name "$VERSION_NAME" \
    --auto-add-overlay

echo "▸ copying assets"
cp -r "$ROOT/app/src/main/assets/." "$OUT/assets/"

echo "▸ compiling java"
find "$ROOT/app/src/main/java" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
# --release 8 keeps the java.* surface at Java 8 level (all of it exists on
# API 26) while android.jar provides the platform classes. D8 desugars the
# lambdas afterwards.
javac --release 8 \
    -classpath "$ANDROID_JAR" \
    -encoding UTF-8 \
    -nowarn \
    -d "$OUT/classes" \
    @"$OUT/sources.txt" 2>&1 | grep -v 'bootstrap class path\|source value 8\|target value 8\|deprecat' || true

if [ ! -d "$OUT/classes/com" ]; then
    echo "✗ java compilation failed"
    exit 1
fi

echo "▸ dexing"
java -cp "$D8" com.android.tools.r8.D8 \
    --release \
    --min-api "$MIN_SDK" \
    --lib "$ANDROID_JAR" \
    --output "$OUT/dex" \
    $(find "$OUT/classes" -name '*.class')

echo "▸ packaging"
python3 - "$OUT" <<'PY'
import os, shutil, sys, zipfile

out = sys.argv[1]
src = os.path.join(out, 'base.apk')
dst = os.path.join(out, 'unsigned.apk')

with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        zout.writestr(item, zin.read(item.filename))
    zout.write(os.path.join(out, 'dex', 'classes.dex'), 'classes.dex')
    assets = os.path.join(out, 'assets')
    for name in sorted(os.listdir(assets)):
        zout.write(os.path.join(assets, name), 'assets/' + name)
print('packaged')
PY

echo "▸ aligning"
"$ZIPALIGN" -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo "▸ signing"
# The key lives outside build/ so every rebuild keeps the same certificate and
# new versions can be installed over old ones.
KEYSTORE="$ROOT/keystore/dotpray.keystore"
mkdir -p "$ROOT/keystore"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v -keystore "$KEYSTORE" \
        -storepass android -keypass android -alias dotpray \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Dotpray Debug, OU=App, O=Dotpray, L=Unknown, ST=Unknown, C=US" >/dev/null 2>&1
fi

java -jar "$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --ks-key-alias dotpray \
    --min-sdk-version "$MIN_SDK" \
    --out "$ROOT/dist/$APK_NAME" \
    "$OUT/aligned.apk"

java -jar "$APKSIGNER" verify --print-certs "$ROOT/dist/$APK_NAME" | head -5

echo "▸ checking ABI coverage"
python3 "$ROOT/tools/check_abi.py" "$ROOT/dist/$APK_NAME"

echo ""
echo "✓ $ROOT/dist/$APK_NAME"
ls -lh "$ROOT/dist/$APK_NAME"
