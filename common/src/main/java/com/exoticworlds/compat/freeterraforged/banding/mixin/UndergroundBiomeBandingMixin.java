package com.exoticworlds.compat.freeterraforged.banding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.freeterraforged.RtfClimateCompression;
import com.exoticworlds.compat.freeterraforged.RtfEntry;
import com.exoticworlds.compat.freeterraforged.RtfLap;
import com.exoticworlds.compat.freeterraforged.banding.BandingLattice;

import etcodehome.freeterraforged.world.worldgen.biome.UndergroundBiomeBanding;

@Mixin(value = UndergroundBiomeBanding.class, remap = false)
public abstract class UndergroundBiomeBandingMixin {
    private static final String SAMPLE = "sample3d";

    private static final String CELL_HASH =
            "Letcodehome/freeterraforged/world/worldgen/biome/UndergroundBiomeBanding;cellHash(JIIIJ)J";

    private static final int BLOCK_X = 0;

    private static final int BLOCK_Z = 2;

    private static final int HORIZONTAL_SIZE = 3;

    @ModifyVariable(method = SAMPLE, at = @At("STORE"), name = "x")
    private static double toroidal$latticeX(double x, @Local(argsOnly = true, ordinal = BLOCK_X) int blockX,
            @Local(argsOnly = true, ordinal = BLOCK_Z) int blockZ,
            @Local(argsOnly = true, ordinal = HORIZONTAL_SIZE) int horizontalSize) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null
                ? x
                : BandingLattice.x(frame, RtfClimateCompression.of(RtfEntry.generationFold()), blockX, blockZ,
                        horizontalSize);
    }

    @ModifyVariable(method = SAMPLE, at = @At("STORE"), name = "z")
    private static double toroidal$latticeZ(double z, @Local(argsOnly = true, ordinal = BLOCK_Z) int blockZ,
            @Local(argsOnly = true, ordinal = HORIZONTAL_SIZE) int horizontalSize) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null
                ? z
                : BandingLattice.z(frame, RtfClimateCompression.of(RtfEntry.generationFold()), blockZ, horizontalSize);
    }

    @WrapOperation(method = SAMPLE, at = @At(value = "INVOKE", target = CELL_HASH))
    private static long toroidal$foldedCell(long seed, int x, int y, int z, long salt, Operation<Long> original,
            @Local(argsOnly = true, ordinal = HORIZONTAL_SIZE) int horizontalSize) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(seed, x, y, z, salt);
        }

        double compression = RtfClimateCompression.of(RtfEntry.generationFold());
        return original.call(seed, BandingLattice.foldX(frame, compression, x, z, horizontalSize), y,
                BandingLattice.foldZ(frame, compression, z, horizontalSize), salt);
    }
}
