package com.exoticworlds.client.shape.feature;

@FunctionalInterface
public interface FeatureView<T> {
    void extend(FeaturePanel<T> panel);
}
