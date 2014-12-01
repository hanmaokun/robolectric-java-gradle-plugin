package org.github.bademux.gradle

import com.android.builder.core.DefaultManifestParser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test

class RobolectricJavaPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.plugins.apply JavaPlugin
        project.extensions.create('robolectric', RobolectricJavaPluginExtension)

        project.afterEvaluate { p -> process(p) }

    }

    private static void process(final Project project) {
        def ext = project.extensions.robolectric
        project.evaluationDependsOn(ext.androidProjectName)

        project.project(ext.androidProjectName) { aproject ->
            configureProject(project,
                             check(aproject, ext.flavorName, ext.buildType),
                             getPackageName(aproject.android))
        }
    }

    private static def check(final Project aproject,
                             final String flavorName, final String buildType) {
        if (!aproject.hasProperty('android')) {
            throw new IllegalArgumentException("'${aproject.name}' is invalid android project")
        }
        def variant = aproject.android.applicationVariants.find {
            it.variantData.capitalizedBuildTypeName.equals(buildType) &&
            it.variantData.capitalizedFlavorName.equals(flavorName)
        }
        if (variant == null) {
            throw new IllegalArgumentException(
                    "Unknown flavorName:${flavorName} or buildType:${buildType}")
        }
        return variant
    }

    private static void configureProject(final Project project, variant, String packageName) {
        Task processResourcesTask = variant.outputs.get(0).processResources
        //add dependency
        project.tasks.processTestResources.dependsOn processResourcesTask
        project.tasks.compileTestJava.dependsOn variant.javaCompile

        final Test testTask = project.tasks.getByName('test')
        testTask.description = 'Runs all Robolectric tests'
        testTask.systemProperties += ['android.package'  : packageName,
                                      'android.manifest' : processResourcesTask.manifestFile,
                                      'android.resources': processResourcesTask.resDir,
                                      'android.assets'   : processResourcesTask.assetsDir]

        project.logger.debug(testTask.systemProperties.toMapString())

        project.dependencies.add('testCompile', project.files(variant.javaCompile.destinationDir))

    }

    private static String getPackageName(android) {
        String packageName = android.defaultConfig.applicationId
        if (packageName != null && !packageName.isEmpty()) {
            return packageName
        }
        return new DefaultManifestParser().getPackage(android.sourceSets.main.manifest.srcFile)
    }
}

class RobolectricJavaPluginExtension {

    void useWith(final String androidProjectName,
                 final String flavorName, final String buildType) {
        this.androidProjectName = androidProjectName
        this.flavorName = flavorName.capitalize()
        this.buildType = buildType.capitalize()
    }


    String androidProjectName

    String flavorName

    String buildType
}