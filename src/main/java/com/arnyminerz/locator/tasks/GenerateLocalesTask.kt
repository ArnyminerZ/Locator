package com.arnyminerz.locator.tasks

import com.arnyminerz.locator.LocatorPlugin
import groovy.util.IndentPrinter
import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.withGroovyBuilder
import java.io.File
import java.io.FileFilter
import java.util.*

@CacheableTask
abstract class GenerateLocalesTask : DefaultTask() {
    companion object {
        /**
         * Matches all language qualifiers with a region of three characters, which is not supported
         * by Java's Locale.
         * @see resourceQualifierToLanguageTag
         */
        private val langRegex = Regex(".*-.{3}")

        /**
         * Converts the language qualifier given from Android to Java Locale language tag.
         * @param lang The qualifier to convert. Example: `en`, `zh-rTW`...
         * @return A correct language code to be introduced into [java.util.Locale.forLanguageTag].
         */
        private fun resourceQualifierToLanguageTag(lang: String): String {
            // If the language qualifier is correct, return it
            if (!lang.matches(langRegex)) return lang
            // Otherwise, fix it
            val hyphenIndex = lang.indexOf('-')
            // Remove the first character of the 3 (rGB -> GB, rTW -> TW)
            return lang.substring(0, hyphenIndex) + "-" + lang.substring(hyphenIndex + 2)
        }

        private fun Project.getLocalesForFlavor(flavorDir: File): Set<String>? {
            if (!flavorDir.exists()) {
                logger.debug("Tried to get locales for non-existing flavor. Directory: $flavorDir")
                return null
            }

            val locales = linkedSetOf("en")

            logger.debug("Getting locales values directories from $flavorDir")
            flavorDir.listFiles(FileFilter {
                it.isDirectory && it.name.startsWith("values-") && File(it, "strings.xml").exists()
            })?.forEach { directory ->
                val dirName = directory.name
                val langCode = dirName.substring(dirName.indexOf('-') + 1)
                val fixedLangCode = resourceQualifierToLanguageTag(langCode)
                locales.add(fixedLangCode)
            }

            logger.info("Supported locales: ${locales.joinToString(", ")}")

            return locales
        }

        fun Project.getLocales(name: String, dir: File): Set<String> {
            logger.debug("Getting locales for $name ($dir)")
            val locales = getLocalesForFlavor(dir)

            logger.debug("Locales found for ${name}: $locales")
            return locales ?: emptySet()
        }
    }

    data class TaskInput(
        @Input val variantName: String,
        @Input val buildType: String?,
        @Input val productFlavors: List<String>,
        @Input val packageName: String,
        @Internal val sourceSetLocales: Map<String, Set<String>>,
    )

    private fun generateLocalesConfig(locales: Set<String>, srcDir: File) {
        logger.debug("Creating locales output dir...")
        val outputDir = File(srcDir, "xml")
        outputDir.mkdirs()

        logger.debug("Generating locales_config.xml ($outputDir) for ${locales.size} locales...")
        File(outputDir, "locales_config.xml").writer().use { writer ->
            val xml = MarkupBuilder(IndentPrinter(writer, "    ", true, true))
            xml.mkp.apply {
                xmlDeclaration(mapOf("version" to "1.0", "encoding" to "utf-8"))
                comment("Generated at ${Date()}")
                yield("\r\n")
            }
            xml.withGroovyBuilder {
                "locale-config"(mapOf("xmlns:android" to "http://schemas.android.com/apk/res/android")) {
                    locales.forEach { locale ->
                        "locale"("android:name" to locale)
                    }
                }
            }
        }
    }

    private fun generateLocalesObject(locales: Set<String>, kotlinDir: File) {
        logger.debug("Creating locales kotlin dir...")
        val sourceSetDir = File(kotlinDir, input.packageName.replace('.', '/'))
        sourceSetDir.mkdirs()

        File(sourceSetDir, "Locator.kt").writer().use { writer ->
            writer.write("package ${input.packageName}\n\n")
            writer.write("object Locator {\n")
            val localesKeysStr = locales.joinToString { "\"$it\"" }
            writer.write("  /**\n")
            writer.write("   * Provides a list of the keys of the locales available.\n")
            writer.write("   */\n")
            writer.write("  val LocalesKeys: Array<String> = arrayOf($localesKeysStr)\n")
            val localesStr =
                locales.joinToString("") { "\n    java.util.Locale.forLanguageTag(\"$it\")," }
            writer.write("\n")
            writer.write("  /**\n")
            writer.write("   * Provides a list of all the locales available converted to Locale.\n")
            writer.write("   */\n")
            writer.write("  val Locales: Array<java.util.Locale> = arrayOf($localesStr\n  )\n")
            writer.write("}\n")
        }
    }

    @Nested
    lateinit var input: TaskInput

    fun initialize(input: TaskInput) {
        this.input = input
    }

    @TaskAction
    fun runTask() {
        input.sourceSetLocales.forEach { (sourceSetName, locales) ->
            logger.debug("Locales: $locales")
            generateLocalesConfig(
                locales,
                File(
                    LocatorPlugin.baseDirForVariantName(project, sourceSetName),
                    "src",
                )
            )
            generateLocalesObject(
                locales,
                File(
                    LocatorPlugin.baseDirForVariantName(project, sourceSetName),
                    "java",
                ),
            )
        }
    }
}
