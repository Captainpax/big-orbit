import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException

plugins {
    id("com.android.application") version "9.4.0" apply false
}

group = "com.littleorbit"
version = "1.0.0"

val signingValues = listOf(
    "BIG_ORBIT_SIGNING_STORE_FILE",
    "BIG_ORBIT_SIGNING_STORE_PASSWORD",
    "BIG_ORBIT_SIGNING_KEY_ALIAS",
    "BIG_ORBIT_SIGNING_KEY_PASSWORD",
).associateWith { providers.environmentVariable(it).orNull }
val missingSigningValues = signingValues.filterValues { it.isNullOrBlank() }.keys

subprojects {
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension> {
            if (missingSigningValues.isEmpty()) {
                val releaseSigning = signingConfigs.create("bigOrbitRelease") {
                    storeFile = rootProject.file(
                        signingValues.getValue("BIG_ORBIT_SIGNING_STORE_FILE")!!,
                    )
                    storePassword = signingValues.getValue("BIG_ORBIT_SIGNING_STORE_PASSWORD")
                    keyAlias = signingValues.getValue("BIG_ORBIT_SIGNING_KEY_ALIAS")
                    keyPassword = signingValues.getValue("BIG_ORBIT_SIGNING_KEY_PASSWORD")
                    enableV1Signing = false
                    enableV2Signing = true
                    enableV3Signing = true
                    enableV4Signing = false
                }
                buildTypes.getByName("release").signingConfig = releaseSigning
            }
        }
        tasks.configureEach {
            if (name in setOf("assembleRelease", "bundleRelease", "packageRelease")
                && missingSigningValues.isNotEmpty()) {
                doFirst {
                    throw GradleException(
                        "Big Orbit release signing is incomplete: " +
                            missingSigningValues.sorted().joinToString(),
                    )
                }
            }
        }
    }
}
