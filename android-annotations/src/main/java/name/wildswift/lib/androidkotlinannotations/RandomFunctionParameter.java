package name.wildswift.lib.androidkotlinannotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by swift
 */
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface RandomFunctionParameter {
    String name();
    Class type();
    boolean nullable() default false;
}
