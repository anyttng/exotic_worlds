package com.exoticworlds.compat.mekanism.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import mekanism.common.lib.math.voxel.VoxelPlane;

import net.minecraft.core.BlockPos;

@Mixin(value = VoxelPlane.class, remap = false)
public interface VoxelPlaneAccessor {
    @Accessor("minCol")
    void exotic_worlds$setMinCol(int minCol);

    @Accessor("maxCol")
    void exotic_worlds$setMaxCol(int maxCol);

    @Accessor("minRow")
    void exotic_worlds$setMinRow(int minRow);

    @Accessor("maxRow")
    void exotic_worlds$setMaxRow(int maxRow);

    @Accessor("outsideSet")
    Set<BlockPos> exotic_worlds$outsideSet();
}
