import kurotoon.gradle.extensions.alias
import kurotoon.gradle.extensions.kurox
import kurotoon.gradle.extensions.libs
import kurotoon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidTest : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.test)
            alias(kurox.plugins.android.base)
        }
    }
}
