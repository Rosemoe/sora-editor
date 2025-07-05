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

import com.android.build.gradle.BaseExtension
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
    id("build-logic.root-project")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.publish) apply false
}

val highApiProjects = arrayOf("editor-lsp")

fun Project.configureBaseExtension() {
    extensions.findByType(BaseExtension::class)?.run {
        compileSdkVersion(Versions.compileSdkVersion)
        buildToolsVersion = Versions.buildToolsVersion

        defaultConfig {
            minSdk =
                if (highApiProjects.contains(this@configureBaseExtension.name)) Versions.minSdkVersionHighApi else Versions.minSdkVersion
            targetSdk = Versions.targetSdkVersion
            versionCode = Versions.versionCode
            versionName = Versions.versionName
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

fun Project.configureKotlinExtension() {
    extensions.findByType(KotlinAndroidProjectExtension::class)?.run {
        jvmToolchain(17)
    }
}

subprojects {
    group = "io.github.rosemoe"
    version = Versions.versionName

    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
    plugins.withId("org.jetbrains.kotlin.android") {
        configureKotlinExtension()
    }

    plugins.withId("com.vanniktech.maven.publish.base") {
        configure<MavenPublishBaseExtension> {
            group = "io.github.rosemoe"
            version = Versions.versionName
            pomFromGradleProperties()
            publishToMavenCentral()
            signAllPublications()
            if ("editor-bom" != this@subprojects.name) {
                configure(
                    AndroidSingleVariantLibrary(
                        variant = "release",
                        sourcesJar = true,
                        publishJavadocJar = false
                    )
                )
            }
        }
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}
