plugins {
    id("com.android.library")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
}

android {
    buildFeatures { compose = true }
    namespace = "android.zero.studio.widget.editor.symbolinput"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.google.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.gson)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.recyclerview)

    implementation(libs.common.editor)
    api(libs.androidx.annotation)

}