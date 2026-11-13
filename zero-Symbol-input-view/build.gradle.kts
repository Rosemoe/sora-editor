plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish.base")
}

android {
    namespace = "android.zero.studio.widget.editor.symbolinput"
}

dependencies {
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    implementation(projects.editor)
    api(libs.androidx.annotation)
    implementation(projects.languageJava)
    implementation(projects.languageTextmate)

}
