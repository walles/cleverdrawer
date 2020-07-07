/*
 * MIT License
 *
 * Copyright (c) 2017 Johan Walles <johan.walles@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

import java.io.ByteArrayOutputStream
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")

    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// During git hook execution some GIT_ variables are set which make our commands fail unless we
// strip those variables out
fun getNoGitEnv(): Map<String, String> {
    val noGitEnv = HashMap<String, String>(System.getenv())
    val iterator = noGitEnv.entries.iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        if (entry.key.startsWith("GIT_")) {
            iterator.remove()
        }
    }

    return noGitEnv
}

// From: http://stackoverflow.com/questions/17097263/automatically-versioning-android-project-from-git-describe-with-android-studio-g
fun getVersionCode(): Int {
    val output = ByteArrayOutputStream()
    exec {
        environment = getNoGitEnv()
        standardOutput = output
        commandLine = listOf("git", "tag", "--list")
    }
    return output.toString().split("\n").size
}

fun getVersionName(): String {
    val output = ByteArrayOutputStream()
    exec {
        environment = getNoGitEnv()
        standardOutput = output
        commandLine = listOf("git", "describe", "--tags", "--dirty", "--first-parent")
    }
    return output.toString().trim()
}

android {
    compileSdkVersion(29)

    defaultConfig {
        applicationId = "com.gmail.walles.johan.cleverdrawer"
        minSdkVersion(19)
        targetSdkVersion(29)
        versionCode = getVersionCode()
        versionName = getVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lintOptions {
        isAbortOnError = true
        isCheckAllWarnings = true
        isWarningsAsErrors = true
        textReport = true
        textOutput = File("stdout")
        isExplainIssues = false

        // These tests can be fine one day and not fine the next without us changing anything. Not
        // reliable enough, disable.
        //
        // Docs at http://tools.android.com/tips/lint-checks
        disable("GradleDependency")
        disable("NewerVersionAvailable")
        disable("OldTargetApi")

        // Johan doesn"t want to generate multiple icon sizes; most phones can resize an icon just
        // fine by themselves.
        disable("IconMissingDensityFolder")

        // Johan doesn"t care about Kotlin. PRs welcome :)
        disable("UnknownNullness")
        disable("KotlinPropertyAccess")

        // We will never autofill anything here (knock, knock)
        disable("Autofill")

        // From <http://tools.android.com/tips/lint-checks>:
        // "This is not important in small projects"
        disable("SyntheticAccessor")
    }

    // NOTE: Must match the value in .travis.yml
    buildToolsVersion = "29.0.3"

    // Read signing properties from ~/.gradle/cleverdrawer.properties
    //
    // From: https://www.timroes.de/2013/09/22/handling-signing-configs-with-gradle/
    if (File("${gradle.gradleUserHomeDir}/cleverdrawer.properties").exists()) {
        val props = Properties()
        props.load(FileInputStream(file("${gradle.gradleUserHomeDir}/cleverdrawer.properties")))

        signingConfigs {
            getByName("release") {
                storeFile = file(props["keystore"])
                storePassword = props["keystore.password"]
                keyAlias = props["keyAlias"]
                keyPassword = props["keyPassword"]
            }
        }

        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.release
            }
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("com.google.android.material:material:1.0.0")
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("org.jetbrains:annotations:17.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.9.3")

    implementation("com.google.firebase:firebase-crashlytics:17.1.1")

    testImplementation("junit:junit:4.12")

    // For Matchers.empty(), see DatabaseUtilsTest.testScoreLaunchablesPerformance()
    testImplementation("org.hamcrest:hamcrest-library:1.3")

    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0")
}
