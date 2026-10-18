package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.ContextOctaves;

public interface ContextOctavesHolder {
    @Nullable ContextOctaves exotic_worlds$contextOctaves();

    void exotic_worlds$contextOctaves(ContextOctaves octaves);
}
