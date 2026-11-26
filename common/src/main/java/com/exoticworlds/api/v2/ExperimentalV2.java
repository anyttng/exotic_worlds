package com.exoticworlds.api.v2;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a part of the API whose contract may still change between releases. The marked parts are the moments that run
 * on a loaded level and its chunks and the rule a shape gives the dimensions that follow its Overworld. Everything
 * without this mark is stable.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ExperimentalV2 {
}
