package org.github.bademux.gradle

import com.android.builder.core.DefaultManifestParser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.JavaPlugin

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

            if (ext.addSdkMavenRepo) {
                addAndroidSdkRepos(project, aproject)
            }
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

        Map robolectricProps = ['android.package'  : packageName,
                                'android.manifest' : processResourcesTask.manifestFile.absolutePath,
                                'android.resources': processResourcesTask.resDir.absolutePath,
                                'android.assets'   : processResourcesTask.assetsDir.absolutePath]
        project.logger.debug('Robolectric conf: %s', robolectricProps)

        //add robolectric properties for test tasks
        // project.tasks.withType(Test) { systemProperties += robolectricProps }
        File configFile = new File(project.sourceSets.test.output.classesDir,
                                   'generated_org.robolectric.Config.properties')
        writeConfigForIde(configFile, robolectricProps, variant.name, project.logger)
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

    private static void addAndroidSdkRepos(Project project, Project aproject) {
        String sdkFolder = aproject.plugins.findPlugin("android").sdkFolder
        project.logger.debug('Android SDK folder: %s', sdkFolder)

        File androidRepo = new File("$sdkFolder/extras/android/m2repository")
        File googleRepo = new File("$sdkFolder/extras/android/m2repository")

        project.buildscript.repositories {
            if (androidRepo.exists()) {
                maven { url androidRepo.toURI() }
            }
            if (androidRepo.exists()) {
                maven { url googleRepo.toURI() }
            }

        }
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

    boolean addSdkMavenRepo = true;
}