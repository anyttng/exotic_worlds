package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.mekanism.MultiblockDataFrame;
import com.exoticworlds.compat.mekanism.MultiblockFrames;

import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.multiblock.MultiblockData;

import net.minecraft.core.BlockPos;

@Mixin(value = MultiblockData.class, remap = false)
public abstract class MultiblockDataMixin implements MultiblockDataFrame {
    @Shadow
    public abstract VoxelCuboid getBounds();

    @Override
    public VoxelCuboid toroidal$bounds() {
        return this.getBounds();
    }

    @ModifyVariable(method = "isKnownLocation", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$seatOntoFrame(BlockPos pos) {
        return MultiblockFrames.seatOnto(this.getBounds(), pos);
    }
}
