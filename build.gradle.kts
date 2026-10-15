// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

spotless {
    kotlin {
        target("app/src/**/*.kt")
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
    }
}

// Include the root formatting checks when verifying the Android module.
project(":app") {
    tasks
        .matching { it.name == "check" }
        .configureEach {
            dependsOn(rootProject.tasks.named("spotlessCheck"))
        }
}
