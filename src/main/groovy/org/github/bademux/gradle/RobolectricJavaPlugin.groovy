package org.github.bademux.gradle

import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.builder.core.DefaultManifestParser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test

class RobolectricJavaPlugin implements Plugin<Project> {

    private Project project

    void apply(final Project project) {
        this.project = project
        def extension = project.extensions.create('robolectric', RobolectricJavaPluginExtension)
        project.evaluationDependsOn(extension.androidProjectName)
        final String buildType = extension.buildType
        final String flavorName = extension.flavorName

        project.plugins.apply JavaPlugin

        project.project(extension.androidProjectName) {
            if (!getProject().hasProperty('android')) {
                throw new IllegalArgumentException("'$name' is invalid android project")
            }
            def variant = android.applicationVariants.find {
                it.variantData.capitalizedBuildTypeName.equals(buildType) &&
                it.variantData.capitalizedFlavorName.equals(flavorName)
            }
            if (variant == null) {
                throw new IllegalArgumentException(
                        "Unknown flavorName:${flavorName} or buildType:${buildType}")
            }

            configureProject(variant, getPackageName(android))
        }
    }

    private void configureProject(variant, String packageName) {
        ProcessAndroidResources processResourcesTask = variant.outputs.get(0).processResources
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

    private String getPackageName(android) {
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