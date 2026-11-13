import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.CommonExtension
import kurotoon.gradle.extensions.alias
import kurotoon.gradle.extensions.android
import kurotoon.gradle.extensions.api
import kurotoon.gradle.extensions.debugApi
import kurotoon.gradle.extensions.implementation
import kurotoon.gradle.extensions.libs
import kurotoon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("UNUSED")
class PluginComposeAndroid : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.kotlin.compose.compiler)
        }

        android {
            buildFeatures {
                compose = true
            }
        }

        dependencies {
            implementation(platform(libs.androidx.compose.bom))

            // Compose @Preview tooling
            api(libs.androidx.compose.uiToolingPreview)
            debugApi(libs.androidx.compose.uiTooling)
        }
    }
}

private fun CommonExtension.buildFeatures(block: BuildFeatures.() -> Unit) {
    buildFeatures.apply(block)
}
