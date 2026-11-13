plugins {
    alias(kurox.plugins.android.library)
    alias(kurox.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "kurotoon.core.metro"
}

dependencies {
    implementation(libs.metro.runtime)
}
