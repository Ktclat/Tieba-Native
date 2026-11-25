import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    autowire(libs.plugins.com.android.application)
    autowire(libs.plugins.kotlin.android)
    autowire(libs.plugins.kotlin.compose)
    autowire(libs.plugins.kotlin.kapt)
    autowire(libs.plugins.kotlin.serialization)
    autowire(libs.plugins.kotlin.parcelize)
    autowire(libs.plugins.hilt.android)
    autowire(libs.plugins.kotlin.ksp)
    autowire(libs.plugins.com.squareup.wire)
}

// Tieba Native is an independent fork: CI debug builds are not upstream releases.
val isSelfBuild = true
// Resolve HEAD once so a concurrent checkout cannot mix one commit's name with another's code.
fun git(vararg args: String): String = providers.exec {
    workingDir(rootDir)
    commandLine("git", *args)
}.standardOutput.asText.get().trim()

val (shallow, sourceCommit) = git("rev-parse", "--is-shallow-repository", "HEAD").lines()
check(shallow == "false") { "Computing Android versionCode requires the full history. Run git fetch --unshallow." }
val applicationVersionName = "${property.versionName}+${sourceCommit.take(12)}"
val applicationVersionCode = Math.addExact(property.versionCodeBase, git("rev-list", "--count", sourceCommit).toInt())

wire {
    sourcePath { srcDir("src/main/protos") }
    kotlin { android = true }
}

android {
    compileSdk = 35
    defaultConfig {
        applicationId = "com.huanchengfly.tieba.post"
        minSdk = 26
        targetSdk = 35
        versionCode = applicationVersionCode
        versionName = applicationVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        manifestPlaceholders["is_self_build"] = "$isSelfBuild"
    }
    buildFeatures { compose = true }
    // Host-side Compose regression tests need the merged Android resources/manifest.
    testOptions { unitTests.isIncludeAndroidResources = true }
    signingConfigs {
        if (property.keystore.file.isNotBlank()) {
            create("config") {
                storeFile = file(File(rootDir, property.keystore.file))
                storePassword = property.keystore.password
                keyAlias = property.keystore.key.alias
                keyPassword = property.keystore.key.password
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }
    buildTypes {
        all {
            signingConfig = if (signingConfigs.any { it.name == "config" })
                signingConfigs.getByName("config") else signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            isJniDebuggable = false
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + project.buildDir.absolutePath + "/compose_metrics")
        freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + project.buildDir.absolutePath + "/compose_metrics")
        freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=" + project.rootDir.absolutePath + "/compose_stability_configuration.txt")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }
    }
    namespace = "com.huanchengfly.tieba.post"
    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            (this as BaseVariantOutputImpl).outputFileName = "Tieba-Native-${applicationVersionName}-${variant.buildType.name}.apk"
        }
        kotlin.sourceSets {
            getByName(variant.name) { kotlin.srcDir("build/generated/ksp/${variant.name}/kotlin") }
        }
    }
}

dependencies {
    implementation(airbnb.lottie)
    implementation(airbnb.lottie.compose)
    implementation(kotlinx.serialization.json)
    implementation(kotlinx.collections.immutable)
    implementation(androidx.media3.exoplayer)
    implementation(androidx.media3.ui)
    implementation(compose.destinations.core)
    ksp(compose.destinations.ksp)
    implementation(androidx.navigation.compose)
    api(wire.runtime)
    implementation(hilt.android)
    kapt(hilt.compiler)
    implementation(androidx.hilt.navigation.compose)
    kapt(androidx.hilt.compiler)
    implementation(accompanist.drawablepainter)
    implementation(accompanist.systemuicontroller)
    implementation(accompanist.placeholder)
    implementation(sketch.core)
    implementation(sketch.compose)
    implementation(sketch.ext.compose)
    implementation(sketch.gif)
    implementation(sketch.okhttp)
    implementation(zoomimage.compose.sketch)
    implementation(compose.bom)
    androidTestImplementation(compose.bom)
    runtimeOnly(compose.runtime.tracing)
    implementation(compose.animation)
    implementation(compose.animation.graphics)
    implementation("androidx.compose.material3:material3:1.4.0-alpha18")
    implementation(compose.material.icons.core)
    implementation(compose.material.icons.extended)
    implementation(compose.ui.util)
    implementation(compose.ui.tooling.preview)
    androidTestImplementation(compose.ui.test.junit4)
    debugRuntimeOnly(compose.ui.test.manifest)
    implementation(androidx.constraintlayout.compose)
    implementation(github.oaid)
    implementation(org.jetbrains.annotations)
    implementation(kotlin.stdlib)
    implementation(kotlin.reflect)
    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.coroutines.android)
    implementation(androidx.lifecycle.runtime)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation(androidx.activity)
    implementation(androidx.activity.compose)
    implementation(androidx.appcompat)
    implementation(androidx.annotation)
    implementation(androidx.browser)
    implementation(androidx.constraintlayout)
    implementation(androidx.core)
    implementation(androidx.core.splashscreen)
    implementation(androidx.datastore.preferences)
    implementation(androidx.gridlayout)
    implementation(androidx.palette)
    implementation(androidx.window)
    implementation(androidx.startup.runtime)
    testImplementation(junit.junit)
    testImplementation(platform(compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(androidx.test.core)
    androidTestImplementation(androidx.test.core)
    androidTestImplementation(androidx.test.ext.junit)
    androidTestImplementation(androidx.test.rules)
    androidTestImplementation(androidx.test.espresso.core)
    androidTestRuntimeOnly(androidx.test.runner)
    implementation(glide.core)
    ksp(glide.ksp)
    implementation(glide.okhttp3.integration)
    implementation(google.material)
    implementation(okhttp3.core)
    implementation(retrofit2.core)
    implementation(retrofit2.converter.wire)
    implementation(google.gson)
    implementation(org.litepal.android.kotlin)
    implementation(github.matisse)
    implementation(xx.permissions)
    implementation(com.gyf.immersionbar.immersionbar)
    implementation(com.github.yalantis.ucrop)
    implementation(com.jakewharton.butterknife)
    kapt(com.jakewharton.butterknife.compiler)
}
