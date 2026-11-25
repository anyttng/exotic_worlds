package com.exoticworlds.shape.feature.client;

@FunctionalInterface
public interface FeatureView<T> {
    void extend(FeaturePanel<T> panel);
}
