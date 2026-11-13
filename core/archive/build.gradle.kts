plugins {
    alias(kurox.plugins.android.library)
    alias(kurox.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "kurotoon.core.archive"
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.archive)
    implementation(libs.unifile)
}
