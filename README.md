robolectric-java-gradle-plugin
==============================

#Test Project: 
[https://github.com/bademux/deckard-gradle] (you should run gradle uploadArchive on robolectric-java-gradle-plugin project first)

#Reqs:
Tested with AndroidStudio 0.8.14, gradle 2.1, android-gradle-pugin 0.13.3

#HowTo:
1. Add new module to you project
 Add to _build.gradle_
```groovy
apply plugin: 'robolectric'

robolectric{
    useWith(':app', 'Flavor2', 'Debug') // use with :app project (android) for flavours Flavor2 and buildType Debug
    //settings('robolectric.offline': true) //optional
}
```
2. Add needed dependencies to _build.gradle_, ex:
```groovy
dependencies {
    testCompile 'org.robolectric:robolectric:2.4'
    testCompile 'junit:junit:4.11'
    //...

    //Robolectric Android Runtime dependencies
    robolectric 'org.robolectric:android-all:4.3_r2-robolectric-0'
    robolectric 'org.json:json:20080701'
    robolectric 'org.ccil.cowan.tagsoup:tagsoup:1.2'
}
```
3. For IntelliJ Idea compatibility [IdeAwareRobolectricTestRunner](https://github.com/bademux/deckard-gradle/blob/master/test-jvm/src/test/java/org/github/bademux/gradle/IdeAwareRobolectricTestRunner.java) should be used
