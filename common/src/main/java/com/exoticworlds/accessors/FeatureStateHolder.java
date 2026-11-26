package com.exoticworlds.accessors;

import java.util.function.Supplier;

import com.exoticworlds.core.feature.GenerationFeature;

public interface FeatureStateHolder {
    <S> S exotic_worlds$featureState(GenerationFeature feature, Supplier<S> created);
}
