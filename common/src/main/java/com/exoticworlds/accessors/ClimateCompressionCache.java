package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.shape.feature.compactbiomes.ClimateCompression.Resolved;

public interface ClimateCompressionCache {
    @Nullable Resolved exotic_worlds$climateCompression();

    void exotic_worlds$climateCompression(Resolved resolved);
}
