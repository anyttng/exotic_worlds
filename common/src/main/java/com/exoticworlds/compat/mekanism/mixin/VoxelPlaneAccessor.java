package com.exoticworlds.compat.mekanism.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import mekanism.common.lib.math.voxel.VoxelPlane;

import net.minecraft.core.BlockPos;

@Mixin(value = VoxelPlane.class, remap = false)
public interface VoxelPlaneAccessor {
    @Accessor("minCol")
    void toroidal$setMinCol(int minCol);

    @Accessor("maxCol")
    void toroidal$setMaxCol(int maxCol);

    @Accessor("minRow")
    void toroidal$setMinRow(int minRow);

    @Accessor("maxRow")
    void toroidal$setMaxRow(int maxRow);

    @Accessor("outsideSet")
    Set<BlockPos> toroidal$outsideSet();
}
