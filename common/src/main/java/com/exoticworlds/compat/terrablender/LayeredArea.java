package com.exoticworlds.compat.terrablender;

import org.jspecify.annotations.Nullable;

public interface LayeredArea {
    void exotic_worlds$register(RegionLayerStack stack, int depth);

    @Nullable RegionLayerStack exotic_worlds$stack();

    int exotic_worlds$depth();
}
