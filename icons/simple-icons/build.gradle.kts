plugins {
    alias(kurox.plugins.android.library)
    alias(kurox.plugins.compose)

    alias(kurox.plugins.spotless)

    alias(libs.plugins.valkyrie)
}

android {
    namespace = "kurotoon.icons.simpleicons"
}

spotless {
    format("svg") {
        target("src/**/*.svg")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

valkyrie {
    packageName = "kurotoon.icons.simpleicons"
    generateAtSync = true

    imageVector {
        suppressUnusedReceiverWarning = true
    }

    iconPack {
        name = "SimpleIcons"
        targetSourceSet = "main"
    }
}

dependencies {
    api(libs.androidx.compose.ui)
}
