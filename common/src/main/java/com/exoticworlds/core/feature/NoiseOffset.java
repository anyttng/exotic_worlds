package com.exoticworlds.core.feature;

@FunctionalInterface
public interface NoiseOffset {
    NoiseOffset NONE = () -> 0.0;

    double get();
}
