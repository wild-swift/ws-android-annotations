/*
 * Copyright (C) 2020 Wild Swift
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package name.wildswift.android.kannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by swift
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ViewField {
    // common properties
    String name();

    // Type
    Class byDelegate() default Void.class;

    ViewProperty byProperty() default ViewProperty.none;

    Class type() default Void.class;
    String defaultValue() default "";
    String childPropertyName() default "";
    String childPropertySetter() default "";

    String childPropertyListener() default "";

    String childPropertyListenerParams() default "newValue";

    //
    ReadWriteMode rwType() default ReadWriteMode.Field;

    // change child properties
    String childName() default "";

    boolean activeChild() default false;
}
