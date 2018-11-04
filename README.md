My annotation processing library

Usage
```
repositories {
    ...
    maven { url  "https://dl.bintray.com/wildswift/general" }
    maven { url "https://kotlin.bintray.com/kotlinx/" }
}
...
dependencies {
    kapt "name.wildswift.android:android-annotation-processor:0.2"
    implementation "name.wildswift.android:android-annotations:0.2"
    
    ...
}
```
