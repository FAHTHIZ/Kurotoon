dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
        create("kurox") {
            from(files("../kuro.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
