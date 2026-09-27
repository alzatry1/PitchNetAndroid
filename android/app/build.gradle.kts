plugins {
    id("com.android.application")
}

android {
    namespace = "com.alzatry1.pitchnet"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.alzatry1.pitchnet"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.6.1-a0.1"

        ndk {
            // v1: 64-bit only. 32-bit can be enabled later for older devices.
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                // libonnxruntime.so is prebuilt against libc++_shared; match it.
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {
            // JUCE's Android Java glue: Gradle does not discover module java
            // dirs automatically, so wire them in explicitly (verified paths).
            java.srcDirs(
                "../../third_party/JUCE/modules/juce_core/native/javacore/init",
                "../../third_party/JUCE/modules/juce_core/native/javacore/app",
                "../../third_party/JUCE/modules/juce_gui_basics/native/java/app",
                "../../third_party/JUCE/modules/juce_gui_basics/native/javaopt/app",
                "../../third_party/JUCE/modules/juce_audio_devices/native/java/app",
                "../../third_party/JUCE/modules/juce_audio_devices/native/javaopt/app"
            )
            // ORT .so files fetched by android/fetch-ort-android.sh
            jniLibs.srcDirs("ort/jniLibs")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
