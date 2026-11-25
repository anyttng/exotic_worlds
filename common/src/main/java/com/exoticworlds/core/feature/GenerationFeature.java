package com.exoticworlds.core.feature;

import com.exoticworlds.core.option.ShapeOptions;

public interface GenerationFeature {
    String getKey();

    default boolean isOn(ShapeOptions options) {
        return true;
    }
}
