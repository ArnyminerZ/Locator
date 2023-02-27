# Locator

An Android Gradle plugin for automating the new localization features introduced together with Android 13.

[![Version badge](https://img.shields.io/gradle-plugin-portal/v/com.arnyminerz.locator?style=for-the-badge)](https://plugins.gradle.org/plugin/com.arnyminerz.locator)

**Note: App Compat `1.6.0-beta01` or higher is required. See [here](https://developer.android.com/guide/topics/resources/app-languages#androidx-impl) for more information.**

## What this library does

- Generating `locale-config.xml` files for each variant and flavor.
- Configure the manifest automatically to take the created locale config file.

# Using

First, add the plugin to your project's classpath:

```groovy
buildscript {
    dependencies {
        classpath("com.arnyminerz.locator:Locator:1.0.2")
    }
}
```

And to the plugins section of your `build.gradle(.kts)` file:

```groovy
plugins {
    id("com.arnyminerz.locator")
}
```

Now build the project, and everything should be ready to go. You can access the locales keys with
`Locator.LocalesKeys`. There's also `Locator.Locales`, which provides the same options, already
converted into `Locale`.
