package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.shape.climate.ClimateCompression.Resolved;

public interface ClimateCompressionCache {
    @Nullable Resolved toroidal$climateCompression();

    void toroidal$climateCompression(Resolved resolved);
}
