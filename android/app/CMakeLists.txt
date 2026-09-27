# PitchNet Android app — native build entry.
#
# Structure: Gradle (externalNativeBuild) invokes this file with the NDK
# toolchain; JUCE is added as a subdirectory and juce_add_gui_app() turns
# into a SHARED library on Android (JUCE 8 CMake Android branch, verified in
# third_party/JUCE/extras/Build/CMake/JUCEUtils.cmake:2221).
#
# The desktop plugin layer (Source/Plugin/) is excluded entirely: there is no
# VST3/AU/ARA host ecosystem on Android, and the standalone app path is what
# we build here.

cmake_minimum_required(VERSION 3.22)
project(PitchNetAndroid LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(PITCHNET_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../..)

# ---------------------------------------------------------------------------
# JUCE 8 (SessionLoops fork, submodule)
# ---------------------------------------------------------------------------
add_subdirectory(${PITCHNET_ROOT}/third_party/JUCE juce)

# ---------------------------------------------------------------------------
# ONNX Runtime (prebuilt Android artifacts)
#   - headers vendored at android/app/ort/include (platform-independent)
#   - libonnxruntime.so downloaded by android/fetch-ort-android.sh into
#     android/app/ort/jniLibs/<abi>/ and packaged via Gradle jniLibs
# ---------------------------------------------------------------------------
set(ORT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/ort)
if(NOT EXISTS ${ORT_DIR}/jniLibs/${ANDROID_ABI}/libonnxruntime.so)
    message(FATAL_ERROR
        "libonnxruntime.so for ABI '${ANDROID_ABI}' not found under "
        "${ORT_DIR}/jniLibs. Run: sh android/fetch-ort-android.sh")
endif()

add_library(onnxruntime SHARED IMPORTED)
set_target_properties(onnxruntime PROPERTIES
    IMPORTED_LOCATION ${ORT_DIR}/jniLibs/${ANDROID_ABI}/libonnxruntime.so)

# ---------------------------------------------------------------------------
# PitchNet application target (shared library on Android)
# ---------------------------------------------------------------------------
juce_add_gui_app(PitchNet
    PRODUCT_NAME "PitchNet"
    COMPANY_NAME "Alzat"
    COMPANY_WEBSITE "https://github.com/alzatry1/PitchNetAndroid"
    BUNDLE_ID "com.alzatry1.pitchnet"
    COMPANY_COPYRIGHT "Copyright 2026"
    MICROPHONE_PERMISSION_ENABLED TRUE
    DOCUMENT_EXTENSIONS "wav;mp3;flac;ogg")

# JUCE's Java loader (com.rmsl.juce.Java) hardcodes
# System.loadLibrary("juce_jni"), so the shared library must be named
# libjuce_jni.so. OUTPUT_NAME is otherwise a no-op on Android.
set_target_properties(PitchNet PROPERTIES OUTPUT_NAME "juce_jni")

# Sources: whole app except the desktop plugin host-integration layer.
file(GLOB_RECURSE PITCHNET_SOURCES CONFIGURE_DEPENDS
    ${PITCHNET_ROOT}/Source/*.cpp)
list(FILTER PITCHNET_SOURCES EXCLUDE REGEX "/Source/Plugin/")
target_sources(PitchNet PRIVATE ${PITCHNET_SOURCES})

# Embedded resources — same set as the desktop build (images + localization
# + third-party notices). Models stay out of the APK for now.
file(GLOB BINARY_DATA_FILES CONFIGURE_DEPENDS
    ${PITCHNET_ROOT}/Resources/images/*)
file(GLOB LANG_FILES CONFIGURE_DEPENDS
    ${PITCHNET_ROOT}/Resources/lang/*.json)
list(SORT LANG_FILES)
list(APPEND BINARY_DATA_FILES ${LANG_FILES})
if(EXISTS ${PITCHNET_ROOT}/THIRD_PARTY_NOTICES.txt)
    list(APPEND BINARY_DATA_FILES ${PITCHNET_ROOT}/THIRD_PARTY_NOTICES.txt)
endif()
juce_add_binary_data(BinaryData SOURCES ${BINARY_DATA_FILES})

target_compile_definitions(PitchNet PRIVATE
    HAVE_ONNXRUNTIME=1
    JUCE_WEB_BROWSER=0
    JUCE_USE_CURL=0
    JUCE_MODAL_LOOPS_PERMITTED=1
    PITCHNET_PROJECT_RESOURCES_DIR="${PITCHNET_ROOT}/Resources")

target_include_directories(PitchNet PRIVATE
    ${PITCHNET_ROOT}/Source
    ${PITCHNET_ROOT}/third_party/r8brain-free-src
    ${ORT_DIR}/include)

target_link_libraries(PitchNet PRIVATE
    BinaryData
    onnxruntime
    juce::juce_gui_basics
    juce::juce_gui_extra
    juce::juce_audio_basics
    juce::juce_audio_devices
    juce::juce_audio_formats
    juce::juce_audio_utils
    juce::juce_audio_processors
    juce::juce_dsp
    juce::juce_recommended_config_flags)

# LTO is deliberately skipped on Android for v1 (debug-ability and build
# speed on CI); enable later for release builds if needed.
