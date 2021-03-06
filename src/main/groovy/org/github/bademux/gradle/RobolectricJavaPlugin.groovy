package org.github.bademux.gradle

import com.android.builder.core.DefaultManifestParser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test

class RobolectricJavaPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.plugins.apply JavaPlugin
        project.extensions.create('robolectric', RobolectricJavaPluginExtension)

        //used to add Robolectric Android Runtime @see maven org.robolectric:android-all
        project.configurations { robolectric }

        project.afterEvaluate { p -> process(p) }
    }

    private static void process(final Project project) {
        def ext = project.extensions.robolectric
        project.evaluationDependsOn(ext.androidProjectName)

        project.project(ext.androidProjectName) { aproject ->
            configureProject(project,
                             check(aproject, ext.flavorName, ext.buildType),
                             getPackageName(aproject.android))

            //project.logger.debug('Android SDK folder: %s', aproject.plugins.findPlugin('android').sdkFolder)
        }
    }

    private static def check(final Project aproject,
                             final String flavorName, final String buildType) {
        if (!aproject.plugins.hasPlugin('android')) {
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
        project.tasks.testClasses.dependsOn(processResourcesTask, variant.javaCompile)
        project.dependencies.add('testCompile', project.files(variant.javaCompile.destinationDir))
        project.dependencies.add('testCompile', variant.javaCompile.classpath)

        Map settings = ['android.package'                 : packageName,
                        'android.manifest'                : processResourcesTask.manifestFile.absolutePath,
                        'android.resources'               : processResourcesTask.resDir.absolutePath,
                        'android.assets'                  : processResourcesTask.assetsDir.absolutePath,
                        'robolectric.dependency.classpath': project.configurations.robolectric.asPath]

        Map extSettings = project.extensions.robolectric.settings
        if (extSettings != null && !extSettings.isEmpty()) {
            settings = project.extensions.robolectric.settings + settings
        }
        project.logger.debug('Robolectric config: %s', settings)

        //add robolectric props for test tasks
        project.tasks.withType(Test) { systemProperties += settings }

        File configFile = new File(project.sourceSets.test.output.classesDir,
                                   'generated_org.robolectric.Config.properties')
        writeConfigForIde(configFile, settings, variant.name, project.logger)
    }

    /** fix tests run for IDE. Should be used with org.github.bademux.gradle.IdeAwareRobolectricTestRunner */
    private static void writeConfigForIde(File propFile, Map prop, String comment, Logger log) {
        try {
            Properties properties = new Properties();
            properties.putAll(prop)
            properties.store(propFile.newWriter(), comment)
            log.debug('Write robolectric conf for IDE: %s', propFile.absolutePath)
        } catch (IOException e) {
            log.debug('exception occured while saving properties file', e)
        }
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

    void settings(Map<String, String> properties) { this.settings = settings }

    protected Map<String, String> settings

}