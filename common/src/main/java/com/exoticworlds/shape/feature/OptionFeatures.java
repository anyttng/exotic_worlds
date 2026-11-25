package com.exoticworlds.shape.feature;

import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.option.ShapeOptions;

public final class OptionFeatures {

    public static ShapeOptions newWorldOptions() {
        ShapeOptions options = ShapeOptions.DEFAULT;
        for (GenerationFeature feature : ShapeFeatures.registeredAt(GenerationFeature.class)) {
            if (feature instanceof OptionFeature<?> offered) {
                options = withNewWorldValue(options, offered);
            }
        }

        return options;
    }

    private static <T> ShapeOptions withNewWorldValue(ShapeOptions options, OptionFeature<T> feature) {
        return options.with(feature.getOption(), feature.getNewWorldValue());
    }

    private OptionFeatures() {
    }
}
