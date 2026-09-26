package com.toroidalworld.compat.terrablender;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.noise.GenerationTransformerContext;

import net.minecraft.core.Direction;

public final class RegionLayerStack {
    private volatile int topDepth;

    private volatile @Nullable RegionLayerFold fold;

    public void raise(int depth) {
        if (depth > this.topDepth) {
            this.topDepth = depth;
        }
    }

    public int fold(Direction.Axis axis, int depth, int coord) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return coord;
        }

        RegionLayerFold cached = this.fold;
        RegionLayerFold resolved = RegionLayerFold.resolve(cached, transformer, this.topDepth);
        if (resolved != cached) {
            this.fold = resolved;
        }

        return resolved.apply(axis, depth, coord);
    }
}
