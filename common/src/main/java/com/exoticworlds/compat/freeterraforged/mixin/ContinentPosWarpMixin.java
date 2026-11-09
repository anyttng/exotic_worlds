package com.exoticworlds.compat.freeterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.terraforged.ContinentWarp;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import etcodehome.freeterraforged.world.worldgen.cell.continent.advanced.AdvancedContinentGenerator;
import etcodehome.freeterraforged.world.worldgen.cell.continent.uplift.UpliftContinentGenerator;
import etcodehome.freeterraforged.world.worldgen.noise.domain.Domain;

@Mixin(value = {AdvancedContinentGenerator.class, UpliftContinentGenerator.class}, remap = false)
public abstract class ContinentPosWarpMixin {
    @Shadow(remap = false)
    protected float frequency;

    @WrapOperation(method = "apply", at = @At(value = "INVOKE",
            target = "Letcodehome/freeterraforged/world/worldgen/noise/domain/Domain;getX(FFI)F"))
    private float exotic_worlds$warpX(Domain warp, float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(warp, x, z, seed);
        }

        float xRatio = ContinentWarp.ratio(frame, Direction.Axis.X, this.frequency);
        float zRatio = ContinentWarp.ratio(frame, Direction.Axis.Z, this.frequency);
        try (RtfLap.Frame.Scope cells = ContinentWarp.enter(frame, xRatio, zRatio)) {
            float u = x / xRatio;
            return x + (original.call(warp, u, z / zRatio, seed) - u) * xRatio;
        }
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE",
            target = "Letcodehome/freeterraforged/world/worldgen/noise/domain/Domain;getZ(FFI)F"))
    private float exotic_worlds$warpZ(Domain warp, float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(warp, x, z, seed);
        }

        float xRatio = ContinentWarp.ratio(frame, Direction.Axis.X, this.frequency);
        float zRatio = ContinentWarp.ratio(frame, Direction.Axis.Z, this.frequency);
        try (RtfLap.Frame.Scope cells = ContinentWarp.enter(frame, xRatio, zRatio)) {
            float v = z / zRatio;
            return z + (original.call(warp, x / xRatio, v, seed) - v) * zRatio;
        }
    }
}
