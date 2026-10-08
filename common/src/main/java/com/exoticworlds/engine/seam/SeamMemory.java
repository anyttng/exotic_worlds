package com.exoticworlds.engine.seam;

import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public final class SeamMemory {
    public static BlockPos canonical(Entity levelSource, BlockPos pos) {
        WorldFold transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        return transformer == null ? pos : transformer.fold(pos);
    }

    private SeamMemory() {
    }
}
