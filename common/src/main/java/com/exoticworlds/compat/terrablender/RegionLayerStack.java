package com.exoticworlds.compat.terrablender;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;

public final class RegionLayerStack {
    private volatile int topDepth;

    private volatile @Nullable RegionLayerFold fold;

    public void raise(int depth) {
        if (depth > this.topDepth) {
            this.topDepth = depth;
        }
    }

    public @Nullable RegionLayerFold resolved() {
        WorldFold worldFold = GenerationFoldContext.context().wrappedFold();
        if (worldFold == null) {
            return null;
        }

        RegionLayerFold cached = this.fold;
        RegionLayerFold resolved = RegionLayerFold.resolve(cached, worldFold, this.topDepth);
        if (resolved != cached) {
            this.fold = resolved;
        }

        return resolved;
    }
}
