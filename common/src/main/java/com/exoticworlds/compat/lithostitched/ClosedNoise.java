package com.exoticworlds.compat.lithostitched;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;

public record ClosedNoise(WorldFold fold, @Nullable LapFastNoise noise) {
}
