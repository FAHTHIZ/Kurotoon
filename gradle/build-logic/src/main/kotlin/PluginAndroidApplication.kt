import kurotoon.gradle.extensions.alias
import kurotoon.gradle.extensions.kurox
import kurotoon.gradle.extensions.libs
import kurotoon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidApplication : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.application)
            alias(kurox.plugins.android.base)
        }
    }
}
