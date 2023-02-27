plugins {
    `kotlin-dsl`
    // `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.1.0"
}

version = "1.0.2"
group = "com.arnyminerz.locator"

pluginBundle {
    website = "https://github.com/ArnyminerZ/Locator"
    vcsUrl = "https://github.com/ArnyminerZ/Locator"
    tags = listOf("android", "language", "locator", "localization", "translation", "13", "tiramisu", "locale_config", "locale")
}

gradlePlugin {
    plugins {
        create("locatorPlugin") {
            id = "com.arnyminerz.locator"
            implementationClass = "com.arnyminerz.locator.LocatorPlugin"
            displayName = "Locator"
            description = "Gradle plugin to simplify the localization process on Android 13+!"
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    /* Example Dependency */
    /* Depend on the android gradle plugin, since we want to access it in our plugin */
    implementation("com.android.tools.build:gradle:7.4.1")

    /* Example Dependency */
    /* Depend on the kotlin plugin, since we want to access it in our plugin */
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")

    /* Depend on the default Gradle API's since we want to build a custom plugin */
    implementation(gradleApi())
    implementation(localGroovy())
}
