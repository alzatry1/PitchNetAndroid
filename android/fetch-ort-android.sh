#!/bin/sh
# Fetch ONNX Runtime Android artifacts for the native build.
# Downloads the official Maven AAR and extracts libonnxruntime.so (and any
# bundled libc++_shared.so) into android/app/ort/jniLibs/arm64-v8a/.
# Headers are already vendored at android/app/ort/include.
set -e

ORT_VERSION=1.19.2
AAR_URL="https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/${ORT_VERSION}/onnxruntime-android-${ORT_VERSION}.aar"

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ORT_DIR="${SCRIPT_DIR}/app/ort"

mkdir -p "${ORT_DIR}/jniLibs/arm64-v8a"

TMP=$(mktemp -d)
trap 'rm -rf "${TMP}"' EXIT

echo "Downloading ${AAR_URL}"
curl -sSL -o "${TMP}/ort.aar" "${AAR_URL}"

# Extract only libonnxruntime.so (the *4j_jni variant is the Java API binding,
# not needed for pure C++ usage and would bloat the APK).
unzip -o -q "${TMP}/ort.aar" "jni/arm64-v8a/libonnxruntime.so" -d "${TMP}/aar"
cp -v "${TMP}/aar/jni/arm64-v8a/libonnxruntime.so" "${ORT_DIR}/jniLibs/arm64-v8a/"

echo "Extracted:"
ls -la "${ORT_DIR}/jniLibs/arm64-v8a/"
