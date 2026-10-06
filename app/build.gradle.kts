plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
    alias(libs.plugins.room)
}

android {
    namespace = "com.andrecoura.homemonitor"
    compileSdk { version = release(37) }
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.andrecoura.homemonitor"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "com.andrecoura.homemonitor.HiltTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = false
        lintConfig = file("lint.xml")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
        // Robolectric reflects into JDK internals, which JDK 17+ (we run on 25) closes by default.
        unitTests.all {
            it.jvmArgs(
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.konsist)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // Pinned explicitly: the version Compose pulls in breaks on API 37 (InputManager.getInstance was removed).
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Coverage gate: >= 80% of lines in domain, data and view models.
// Excluded on purpose (see docs/TESTING.md): Compose screens/theme/navigation (covered by instrumented UI tests,
// which Kover does not measure), Hilt/Room generated code, DI modules, and the Application/Activity entry points.
kover {
    reports {
        filters {
            excludes {
                classes(
                    "*Screen*",
                    "*ComposableSingletons*",
                    "com.andrecoura.homemonitor.ui.components.*",
                    "com.andrecoura.homemonitor.ui.theme.*",
                    "com.andrecoura.homemonitor.ui.navigation.*",
                    "com.andrecoura.homemonitor.di.*",
                    "com.andrecoura.homemonitor.MainActivity*",
                    "com.andrecoura.homemonitor.HomeMonitorApp*",
                    "*_Impl*",
                    "*_Factory*",
                    "*_HiltModules*",
                    "*Hilt_*",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.internal.*",
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                )
            }
        }
        verify {
            rule("Lines in domain, data and view models") {
                minBound(80)
            }
        }
    }
}
