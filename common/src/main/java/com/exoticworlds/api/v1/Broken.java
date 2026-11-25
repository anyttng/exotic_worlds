package com.exoticworlds.api.v1;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a part of {@code api.v1} that still compiles and still registers but no longer does anything in the game: a
 * mod built against it loads, and what it declared here has no effect. {@link #value()} says what stopped and what to
 * use instead. Everything in {@code api.v1} without this mark works as its documentation says.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Broken {
    /** What no longer happens, and what replaces it. */
    String value();
}
