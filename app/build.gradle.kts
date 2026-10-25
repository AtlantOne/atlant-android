import java.util.jar.JarInputStream
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Local UI tests use an isolated package/output and never load the ARM VPN library.
val localUiTest = providers.gradleProperty("atlantUiTest").orNull == "true"
val atlantAar = providers.gradleProperty("atlantAar").orElse("atlant.aar").get()
val requestedAbis = providers.gradleProperty("atlantAbis").orElse("arm64-v8a").get().split(',')
require(requestedAbis.isNotEmpty() && requestedAbis.all { it in setOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64") }) { "Unsupported Android ABI" }
val splitPublicApks = providers.gradleProperty("atlantSplitApks").orNull == "true"
require(Regex("[A-Za-z0-9_.-]+\\.aar").matches(atlantAar)) { "atlantAar must be a library filename" }
layout.buildDirectory.set(rootProject.layout.projectDirectory.dir(if (localUiTest) "artifacts/build/android/ui-test" else "artifacts/build/android/app"))

val verifyAtlantLibrary = tasks.register("verifyAtlantLibrary") {
    val library = rootProject.layout.projectDirectory.file("artifacts/android/$atlantAar")
    inputs.file(library)
    doLast {
        val hint = "Rebuild the Android library with scripts/build-aar.ps1; Kotlin requires AppRouter API."
        check(library.asFile.isFile) { "Missing $atlantAar. $hint" }
        ZipFile(library.asFile).use { aar ->
            val classes = aar.getEntry("classes.jar") ?: error("Invalid $atlantAar: no classes.jar. $hint")
            JarInputStream(aar.getInputStream(classes)).use { jar ->
                var hasRouter = false
                var hasSetter = false
                var entry = jar.nextJarEntry
                while (entry != null) {
                    when (entry.name) {
                        "atlant/AppRouter.class" -> hasRouter = true
                        "atlant/Atlant.class" -> {
                            val api = jar.readBytes().toString(Charsets.ISO_8859_1)
                            hasSetter = api.contains("setAppRouter")
                        }
                    }
                    entry = jar.nextJarEntry
                }
                check(hasRouter && hasSetter) { "Outdated $atlantAar: required routing API missing. $hint" }
            }
        }
    }
}

tasks.matching { it.name.startsWith("compile") && it.name.endsWith("Kotlin") }.configureEach {
    dependsOn(verifyAtlantLibrary)
}

android {
    namespace = "com.example.atlantvpn"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.atlantvpn"
        if (localUiTest) applicationIdSuffix = ".uitest"
        minSdk = 24
        targetSdk = 37
        versionCode = 8
        versionName = "2.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(if (localUiTest) listOf("x86_64") else requestedAbis)
        }
    }

    splits {
        abi {
            isEnable = splitPublicApks && !localUiTest
            reset()
            include(*requestedAbis.toTypedArray())
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation(files(rootProject.layout.projectDirectory.file("artifacts/android/$atlantAar")))
    implementation("androidx.compose.material:material-icons-extended")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
