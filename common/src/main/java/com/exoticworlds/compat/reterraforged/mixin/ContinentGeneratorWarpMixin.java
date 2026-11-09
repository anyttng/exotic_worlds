package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.terraforged.ContinentWarp;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.continent.simple.ContinentGenerator;
import raccoonman.reterraforged.world.worldgen.noise.domain.Domain;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

@Mixin(value = ContinentGenerator.class, remap = false)
public abstract class ContinentGeneratorWarpMixin {
    @Shadow
    protected float frequency;

    @WrapOperation(method = {"apply", "getEdgeValue", "getNearestCenter"}, at = @At(value = "INVOKE",
            target = "Lraccoonman/reterraforged/world/worldgen/noise/domain/Domain;getOffset(FFI)J"))
    private long exotic_worlds$warpInCells(Domain warp, float x, float z, int seed, Operation<Long> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(warp, x, z, seed);
        }

        float xRatio = ContinentWarp.ratio(frame, Direction.Axis.X, this.frequency);
        float zRatio = ContinentWarp.ratio(frame, Direction.Axis.Z, this.frequency);
        try (RtfLap.Frame.Scope cells = ContinentWarp.enter(frame, xRatio, zRatio)) {
            long offset = original.call(warp, x / xRatio, z / zRatio, seed);
            return PosUtil.packf(PosUtil.unpackLeftf(offset) * xRatio, PosUtil.unpackRightf(offset) * zRatio);
        }
    }
}
