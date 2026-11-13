package kurotoon.gradle.configurations

import com.gradleup.tapmoc.configureJavaCompatibility
import kurotoon.gradle.extensions.kurox
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions

fun Project.configureKotlin() {
    configureJavaCompatibility(kurox.versions.java.get().toInt())
}

@Suppress("Unused")
private fun Project.kotlin(block: KotlinBaseExtension.() -> Unit) {
    extensions.configure(block)
}

@Suppress("Unused")
private fun KotlinBaseExtension.compilerOptions(block: KotlinCommonCompilerOptions.() -> Unit) {
    if (this is HasConfigurableKotlinCompilerOptions<*>) compilerOptions(block)
}
