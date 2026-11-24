package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.ContextOctaves;

public interface ContextOctavesHolder {
    @Nullable ContextOctaves exotic_worlds$getContextOctaves();

    void exotic_worlds$setContextOctaves(ContextOctaves octaves);
}
