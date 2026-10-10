package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.reterraforged.RtfLap;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

@Mixin(value = NoiseUtil.class, remap = false)
public class NoiseUtilMixin {
    private static final int X_ORDINAL = 1;

    private static final int Z_ORDINAL = 2;

    @ModifyVariable(method = {"coord2D", "coord2D_24", "valCoord2D", "hash2D"}, at = @At("HEAD"), argsOnly = true,
            ordinal = X_ORDINAL)
    private static int toroidal$wrapLatticeX(int x, @Local(argsOnly = true, ordinal = Z_ORDINAL) int z) {
        return RtfLap.wrapLatticeX(x, z);
    }

    @ModifyVariable(method = {"coord2D", "coord2D_24", "valCoord2D", "hash2D"}, at = @At("HEAD"), argsOnly = true,
            ordinal = Z_ORDINAL)
    private static int toroidal$wrapLatticeZ(int z) {
        return RtfLap.wrapLatticeZ(z);
    }
}
