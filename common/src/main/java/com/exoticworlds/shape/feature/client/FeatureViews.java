package com.exoticworlds.shape.feature.client;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.registry.StartupRegistry;

public final class FeatureViews {
    private static final StartupRegistry<String, FeatureView<?>> VIEWS = new StartupRegistry<>("Feature views");

    public static <T> void register(ShapeOption<T> option, FeatureView<T> view) {
        VIEWS.register(option.key(), view);
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable FeatureView<T> of(ShapeOption<T> option) {
        return (FeatureView<T>) VIEWS.getEntries().get(option.key());
    }

    private FeatureViews() {
    }
}
