package name.wildswift.lib.androidkotlinannotations;

/**
 * Created by swift
 */

public @interface ActivityField {
    String name();

    Class type();

    boolean nullable() default false;
}
