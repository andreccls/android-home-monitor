plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.room) apply false
}

// ktlint runs as the plain CLI through JavaExec: no Gradle plugin to keep in step with AGP.
val ktlint = configurations.create("ktlint")

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
    }
}

val ktlintFiles = arrayOf("app/src/**/*.kt", "*.kts", "app/*.kts", "!**/build/**")

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks Kotlin code style with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(*ktlintFiles)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Formats Kotlin code with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F", *ktlintFiles)
}
