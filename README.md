# Locator

An Android Gradle plugin for automating the new localization features introduced together with Android 13.

**Note: App Compat `1.6.0-beta01` or higher is required. See [here](https://developer.android.com/guide/topics/resources/app-languages#androidx-impl) for more information.**

## What this library does

- Generating `locale-config.xml` files for each variant and flavor.

## What this library (still) doesn't

- Configure the manifest automatically to take the created locale config file (#1).

# Using

First, add the plugin to your project's classpath:

```groovy
buildscript {
    dependencies {
        classpath("com.arnyminerz.locator:Locator:1.0.0")
    }
}
```

And to the plugins section of your `build.gradle(.kts)` file:

```groovy
plugins {
    id("com.arnyminerz.locator")
}
```

Now go to your application's manifest, and select the `locale-config`, for example:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools">

    <application
            android:allowBackup="true"
            android:dataExtractionRules="@xml/data_extraction_rules"
            android:fullBackupContent="@xml/backup_rules"
            android:icon="@mipmap/ic_launcher"
            android:label="@string/app_name"
            android:roundIcon="@mipmap/ic_launcher_round"
            android:supportsRtl="true"
            android:theme="@style/Theme.Locator"
            android:localeConfig="@xml/locales_config"
            tools:targetApi="31">
        <activity
                android:name=".MainActivity"
                android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>

                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Now build the project, and everything should be ready to go. You can access the locales keys with
`Locator.LocalesKeys`. There's also `Locator.Locales`, which provides the same options, already
converted into `Locale`.
