package name.wildswift.lib.androidkotlinannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by swift
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RandomFunction {
    String perfix() default "random";
    int count() default 1;
    RandomFunctionType type() default RandomFunctionType.boolCheck;
    RandomFunctionParameter[] parameters() default {};
    String[] dictionary();
}
