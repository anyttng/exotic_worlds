package com.exoticworlds.core.feature;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.option.ShapeOptions;

public interface GenerationFeature {
    String getKey();

    default boolean isOn(ShapeOptions options) {
        return true;
    }

    default <F extends GenerationFeature> @Nullable F asMoment(Class<F> moment) {
        return moment.isInstance(this) ? moment.cast(this) : null;
    }
}
