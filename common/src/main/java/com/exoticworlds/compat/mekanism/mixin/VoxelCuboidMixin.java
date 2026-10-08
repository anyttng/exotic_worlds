package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.mekanism.MultiblockBoundsFrame;
import com.exoticworlds.compat.mekanism.MultiblockFrames;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import mekanism.common.lib.math.voxel.VoxelCuboid;

import net.minecraft.core.BlockPos;

@Mixin(value = VoxelCuboid.class, remap = false)
public class VoxelCuboidMixin implements MultiblockBoundsFrame {
    @Unique
    private WorldFold toroidal$fold = WorldFolds.NOOP;

    @Override
    public WorldFold toroidal$fold() {
        return this.toroidal$fold;
    }

    @Override
    public void toroidal$setFold(WorldFold fold) {
        this.toroidal$fold = fold;
    }

    @ModifyVariable(
            method = {
                "getRelativeLocation(Lnet/minecraft/core/BlockPos;)Lmekanism/common/lib/math/voxel/VoxelCuboid$CuboidRelative;",
                "getMatches(Lnet/minecraft/core/BlockPos;)I",
                "getSide(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Direction;"
            },
            at = @At("HEAD"),
            argsOnly = true,
            require = 3)
    private BlockPos toroidal$seatOntoFrame(BlockPos pos) {
        return MultiblockFrames.seatOnto((VoxelCuboid) (Object) this, pos);
    }
}
