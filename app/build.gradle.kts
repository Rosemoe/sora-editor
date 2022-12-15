/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    defaultConfig {
        applicationId = "io.github.rosemoe.sora.app"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = Versions.versionCode
        versionName = Versions.versionName + "-" + System.currentTimeMillis()
    }
    signingConfigs {
        create("general") {
            storeFile = file("../debug.jks")
            storePassword = "114514"
            keyAlias = "debug"
            keyPassword = "114514"
            this.enableV1Signing = true
            this.enableV2Signing = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("general")
            proguardFiles("proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("general")
            proguardFiles("proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    androidResources {
        additionalParameters.add("--warn-manifest-validation")
    }
    buildFeatures {
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    packagingOptions {
        resources.pickFirsts.addAll(
            arrayOf(
                "license/README.dom.txt",
                "license/LICENSE.dom-documentation.txt",
                "license/NOTICE",
                "license/LICENSE.dom-software.txt",
                "license/LICENSE",
            )
        )
    }
}

dependencies {
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-instantapps:18.0.1")

    // Desugar
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    // androidx & material
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

    // Editor
    implementation(projects.editor)
    implementation(projects.languageJava)
    implementation(projects.languageTextmate)
    implementation(projects.editorLsp)
    implementation(projects.languageTreesitter)

    // Tree-sitter languages
    implementation("io.github.itsaky:tree-sitter-java:${Versions.tsBindingVersion}")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Lua language server
    implementation(fileTree("dir" to "libs", "includes" to listOf("*.jar")))
    implementation ("org.eclipse.lsp4j:org.eclipse.lsp4j:${Versions.lsp4jVersion}")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
}
