package com.exoticworlds.compat.terrablender;

import org.jspecify.annotations.Nullable;

public interface LayeredArea {
    void toroidal$register(RegionLayerStack stack, int depth);

    @Nullable RegionLayerStack toroidal$stack();

    int toroidal$depth();
}
