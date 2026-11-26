package com.exoticworlds.shape.feature;

import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.ShapeFeatures;

public abstract class ShapeFeature implements GenerationFeature {
    private final String key;

    protected ShapeFeature(String key) {
        this.key = key;
    }

    @Override
    public final String getKey() {
        return this.key;
    }

    public void register() {
        ShapeFeatures.register(this);
    }
}
