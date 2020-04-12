# Library for generating boilerplate code

## Usage

### Add Kotlin support
Add following lines to your build script (_build.gradle_)
```groovy
buildscript {
    ext.kotlin_version = '1.3.50'
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.71"
    }
}

apply plugin: 'kotlin-android'
apply plugin: 'kotlin-android-extensions'
```
:warning: *It is important to use kotlin-android-extensions plugin, if you want to generate views*


### Add Kotlin annotation processor support
Add following lines to your build script (_build.gradle_)
```groovy
apply plugin: 'kotlin-kapt'
```


### Add dependencies and annotation processor
For usage this library add following dependencies to your build script (_build.gradle_)
```groovy
dependencies {
    kapt "name.wildswift.android:android-annotation-processor:0.7.0"
    implementation "name.wildswift.android:android-annotations:0.7.0"
}
```

_Optional:_ If you want to use view generation add folowing code to kapt properties section in build script (_build.gradle_)
```groovy
kapt {
    arguments {
        arg("application.id", android.defaultConfig.applicationId)
    }
}
```
_Optional:_ If you use view generation, in many case you will need to use resources names. In this case to avoid mistakes you may use [another my plugin](https://github.com/wild-swift/ws-resource-name-resolver-plugin) that generate classes with constants to all names of resources, that you can use in annotations. For example `IdRNames` or `DrawableRNames`   

## Activity extensions
## Random function invocation
## View generating