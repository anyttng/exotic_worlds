package com.exoticworlds.compat.terrablender;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

public final class RegionLayerStack {
    private volatile int topDepth;

    private volatile @Nullable RegionLayerFold fold;

    public void raise(int depth) {
        if (depth > this.topDepth) {
            this.topDepth = depth;
        }
    }

    public int foldX(int depth, int coordX, int coordZ) {
        RegionLayerFold resolved = resolved();
        return resolved == null ? coordX : resolved.foldX(depth, coordX, coordZ);
    }

    public int foldZ(int depth, int coordZ) {
        RegionLayerFold resolved = resolved();
        return resolved == null ? coordZ : resolved.foldZ(depth, coordZ);
    }

    private @Nullable RegionLayerFold resolved() {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return null;
        }

        RegionLayerFold cached = this.fold;
        RegionLayerFold resolved = RegionLayerFold.resolve(cached, transformer, this.topDepth);
        if (resolved != cached) {
            this.fold = resolved;
        }

        return resolved;
    }
}
