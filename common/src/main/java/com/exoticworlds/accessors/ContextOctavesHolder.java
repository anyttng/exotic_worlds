package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.ContextOctaves;

public interface ContextOctavesHolder {
    @Nullable ContextOctaves toroidal$contextOctaves();

    void toroidal$contextOctaves(ContextOctaves octaves);
}
