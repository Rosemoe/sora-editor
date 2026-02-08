/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
}

android {
    namespace = "io.github.rosemoe.sora.app"

    defaultConfig {
        applicationId = "io.github.rosemoe.sora.app"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        AppSigning.getAppSigningConfigOptional(project)
            .onSuccess {
                create("general") {
                    storeFile = it.storeFile
                    storePassword = it.storePassword
                    keyAlias = it.keyAlias
                    keyPassword = it.keyPassword

                    enableV1Signing = true
                    enableV2Signing = true
                }

                buildTypes.forEach { buildType ->
                    buildType.signingConfig = signingConfigs.getByName("general")
                }
            }.onFailure {
                logger.error("Failed to get signing config. Signing configuration is left as is.")
            }
    }

    for (buildType in buildTypes) {
        buildType.apply {
            isMinifyEnabled = false
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

    packaging {
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
    // Desugar
    coreLibraryDesugaring(libs.desugar)

    // androidx & material
    implementation(libs.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.runtime)

    // Editor
    implementation(projects.editor)
    implementation(projects.languageJava)
    implementation(projects.languageTextmate)
    implementation(projects.languageMonarch)
    implementation(projects.editorLsp)
    implementation(projects.languageTreesitter)
    implementation(projects.onigurumaNative)

    // Tree-sitter languages
    implementation(libs.tree.sitter.java)

    // Monarch Languages
    implementation(libs.monarch.language.pack)

    // Kotlin coroutines
    implementation(libs.kotlinx.coroutines)

    // Lua language server
    implementation(fileTree("dir" to "libs", "includes" to listOf("*.jar")))
    implementation(libs.lsp4j)

    debugImplementation(libs.leakcanary)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}
