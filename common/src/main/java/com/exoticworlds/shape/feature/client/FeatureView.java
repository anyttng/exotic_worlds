package com.exoticworlds.shape.feature.client;

@FunctionalInterface
public interface FeatureView<T> {
    void extendPanel(FeaturePanel<T> panel);
}
