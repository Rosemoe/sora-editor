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
    id("com.android.library")
    id("com.vanniktech.maven.publish.base")
}

group = "io.github.Rosemoe.sora-editor"
version = Versions.versionName

android {
    namespace = "io.github.rosemoe.sora.langs.textmate"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    compileOnly(projects.editor)
    implementation("com.google.code.gson:gson:${Versions.gsonVersion}")
    implementation("org.jruby.jcodings:jcodings:${Versions.jcodingsVersion}")
    implementation("org.jruby.joni:joni:${Versions.joniVersion}")
    implementation("org.apache.xmlgraphics:batik-css:${Versions.batikCssVersion}")
    implementation("org.apache.xmlgraphics:batik-util:${Versions.batikUtilVersion}")
    implementation("xerces:xercesImpl:${Versions.xercesImplVersion}")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

}
