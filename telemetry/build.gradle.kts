plugins {
    alias(kurox.plugins.android.library)
    alias(kurox.plugins.spotless)
}

// KuroToon collects no analytics or crash reports. This module only keeps the call sites compiling.
android {
    namespace = "kurotoon.telemetry"
}

dependencies {
    implementation(projects.core.common)
}
