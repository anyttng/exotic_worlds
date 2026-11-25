package com.exoticworlds.accessors;

import java.util.List;

import com.exoticworlds.core.feature.NoiseFeature;

public interface NoiseFeatureHolder {
    List<NoiseFeature> exotic_worlds$getNoiseFeatures();

    void exotic_worlds$setNoiseFeatures(List<NoiseFeature> features);
}
