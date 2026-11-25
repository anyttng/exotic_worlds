package com.exoticworlds.api.v2;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a part of the API whose contract may still change between releases: it reaches deep into chunk generation or
 * the save, and no mod but Exotic Worlds itself has built on it yet. Everything without this mark is stable.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ExperimentalV2 {
}
