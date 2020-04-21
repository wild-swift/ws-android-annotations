# Library for generating boilerplate code

## Usage

### Add Kotlin support
Add following lines to your build script (_build.gradle_)
```groovy
buildscript {
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.71"
    }
}

apply plugin: 'kotlin-android'
apply plugin: 'kotlin-android-extensions'
```
> :warning: *It is important to use kotlin-android-extensions plugin, if you want to generate views*


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
> :warning: _IN PROGRESS_

## Random function invocation
For some reason you may need to inline one of different code block with same behavior. For example if you need to complicate reverse engineering of compiled code. You may annotate class with RandomFunction, and annotation processing task generate inline functions that will call random code from provided dictionary, and this code will changes every time you invoke build process    

## View generating
> :warning: _IN PROGRESS_

### Quick start
To create delegate for View you need
* Create class extend `ViewDelegate`
* Annotate it with `ViewWithDelegate`
* Specify view class name, view class parent in annotation

After that view class will be generated with same visibility modifier as Delegate class

> :dove: _Hint:_ if your class ends with Delegate, you may not specify view class name directly. In this case processor set name to view class same as delegate class name, but without "Delegate" suffix

> :dove: _Info:_ layout resource name may be specified directly, but not required. If resource name not specified, processor uses view class name with removed "View" suffix, added "View" prefix and converting it to screaming case   

Sample:
```kotlin
@ViewWithDelegate(parent = FrameLayout::class)
class MyViewDelegate(view: MyView) : ViewDelegate<MyView, Any>(view)
```

### Fields
Every field in specification for generate views has next parameters

* Name
* Type specification (see section below)
* Read-write mode
* Link with child view _(optional)_  

And now supported two types of field: primitive and collections 

### Field typing
Field type include answer for the next questions: 
* What class or type we use for field declaration?
* How we must init field at object creation 
* How we will set value of field to linked child view
* How we will listen changes in child?

There are 3 way to specify field type
* Use standard pattern (text property for text view)
* Use Delegate specification (if you want to use another generated view for delegation)
* Fully custom setup

#### Standard patterns

Table below shows how standard patterns answer to questions above  

Name|Type|Default value|Set property|Listen property changes
----|----|-------------|------------|-----------------------
text| `String` | "" | if(text != $field) setText($field) |  
visibility| `Int` | 0 | visibility = $field |  
textColor| `Int` | 0 | setTextColor($field) |  
checked| `Boolean` | false | isChecked = $field |  
timePickerHour| `Int` | 0 | hour = $field |  
timePickerMinute| `Int` | 0 | minute = $field | 
imageResource| `Int` | 0 | setImageResource($field) |  
imageDrawable| `Drawable?` | null | setImageDrawable($field) |  
backgroundResource| `Int` | 0 | setBackgroundResource($field) |  
backgroundColor| `Int` | 0 | setBackgroundColor($field) |  
backgroundDrawable| `Drawable?` | null | setBackground($field) | 
radioSelect| `Int?` | null | if ($field != null) check($field) else clearCheck() |


##### Specify types by delegate
> :warning: _IN PROGRESS_

##### Direct types specification
> :warning: _IN PROGRESS_

### List fields
> :warning: _IN PROGRESS_

### Read/write modes
Read-write modes differ in the following parameters: 
* Can be accessed outside view class
* Can be assigned outside view class
* Has notify about internal changes
* Has notify about all changes
* Includes in ViewModel class

A detailed description is presented in the table below.

Name|Access outside|Assign outside|Notification type|ViewModel
----|--------------|--------------|-----------------|---------
Private|false|false|none|no
Property|true|false|none|yes (not change internal state)
ObservableProperty|true|false|int. changes|yes (not change internal state)
Field|true|true|none|yes
ObservableField|true|true|int. changes|yes
FullObservableField|true|true|ex. changes|yes
