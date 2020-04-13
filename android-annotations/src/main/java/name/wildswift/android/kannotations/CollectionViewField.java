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

import android.view.View;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface CollectionViewField {
    String name();

    boolean isPublic() default true;

    String childListView() default "";

    ListImplementation listImplementation() default ListImplementation.RecyclerView;

    // Typing
    Class delegateClass() default Void.class;

    Class elementType() default Void.class;

    Class<? extends View> viewForElementClass() default View.class;

    String modelFieldName() default "viewModel";

    String modelSetterName() default "";

}
