package com.arnyminerz.locator

import com.arnyminerz.locator.tasks.GenerateLocalesTask
import com.arnyminerz.locator.tasks.GenerateLocalesTask.Companion.getLocales
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

class LocatorPlugin: Plugin<Project> {
    companion object {
        fun baseDirForVariantName(project: Project, variantName: String) = File(
            project.buildDir,
            "generated/locator/$variantName",
        )
    }

    override fun apply(project: Project) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.finalizeDsl { commonExtension ->
            project.logger.debug("Finalizing DSL...")
            androidComponents.beforeVariants { variantBuilder ->
                variantBuilder.flavorName
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { commonExtension.addGeneratedFilesToSourceSet(project, it) }
                    ?: run {
                        project.logger.info("No flavors available. Adding main...")
                        commonExtension.addGeneratedFilesToSourceSet(project, "main")
                    }
            }
            androidComponents.onVariants { variant ->
                val capsVariantName = variant.name.capitalized()
                val taskName = "localesGeneratorFor$capsVariantName"

                val sourceSetMap = mutableMapOf<String, Set<File>>()

                commonExtension
                    .getQualifiedSourceSetsByName(variant.name)
                    .let { (name, files) -> sourceSetMap.put(name, files) }
                variant.buildType?.let { buildType ->
                    commonExtension
                        .getQualifiedSourceSetsByName(buildType)
                        .let { (name, files) -> sourceSetMap.put(name, files) }
                }
                variant.flavorName
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { flavorName ->
                        commonExtension
                            .getQualifiedSourceSetsByName(flavorName)
                            .let { (name, files) -> sourceSetMap.put(name, files) }
                    }
                commonExtension
                    .getQualifiedSourceSetsByName("main")
                    .let { (_, files) ->
                        sourceSetMap.forEach { (key, sourceFiles) ->
                            val filesList = sourceFiles.toMutableSet()
                            filesList.addAll(files)
                            sourceSetMap.replace(key, filesList)
                        }
                    }

                val sourceSetLocales = sourceSetMap
                    .mapValues {  (name, files) ->
                        project.logger.debug("Getting locales for ${name}...")
                        files.map { project.getLocales(name, it) }.flatten().toSet()
                    }

                project.tasks.register(
                    taskName,
                    GenerateLocalesTask::class.java,
                    object : Action<GenerateLocalesTask> {
                        override fun execute(task: GenerateLocalesTask) {
                            task.initialize(
                                GenerateLocalesTask.TaskInput(
                                    variantName = variant.name,
                                    buildType = variant.buildType,
                                    productFlavors = variant.productFlavors.map { it.first },
                                    packageName = variant.namespace.get(),
                                    sourceSetLocales = sourceSetLocales,
                                )
                            )
                        }
                    },
                )
                project.beforeEvaluate {
                    sourceSetLocales.forEach { (sourceSetName, locales) ->
                        commonExtension.addResourceConfigurations(sourceSetName, locales)
                    }
                }
                project.afterEvaluate {
                    project.tasks
                        .named("preBuild")
                        .configure(object : Action<Task> {
                            override fun execute(task: Task) {
                                task.dependsOn(taskName)
                            }
                        })
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun CommonExtension<*, *, *, *>.addGeneratedFilesToSourceSet(
        project: Project,
        sourceSetName: String
    ) {
        sourceSets {
            val baseDir = baseDirForVariantName(project, sourceSetName)
            project.logger.debug("Adding generated files ($baseDir) for $sourceSetName.")
            findByName(sourceSetName)?.let {
                it.res.srcDir(File(baseDir, "src"))
                it.java.srcDir(File(baseDir, "java"))
            } ?: project.logger.error("Could not find a source set named $sourceSetName")
        }
    }

    @Suppress("UnstableApiUsage")
    private fun CommonExtension<*, *, *, *>.getQualifiedSourceSetsByName(
        sourceSetName: String,
    ): Pair<String, Set<File>> = sourceSets.getByName(sourceSetName).res.let { res ->
        val dirs = (res as DefaultAndroidSourceDirectorySet).srcDirs
        sourceSetName to dirs
    }

    private fun CommonExtension<*, *, *, *>.addResourceConfigurations(
        flavorName: String,
        locales: Collection<String>,
    ) {
        productFlavors {
            findByName(flavorName)
                ?.resourceConfigurations
                ?.addAll(locales)
                ?: System.err.println("Could not find flavor named \"$flavorName\"")
        }
    }
}
