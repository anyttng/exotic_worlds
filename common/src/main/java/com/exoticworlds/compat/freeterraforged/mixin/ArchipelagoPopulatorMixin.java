package com.exoticworlds.compat.freeterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.freeterraforged.FreeTerraForgedInjectionTargets;
import com.exoticworlds.compat.freeterraforged.RtfLap;

import etcodehome.freeterraforged.world.worldgen.cell.terrain.populator.ArchipelagoPopulator;
import etcodehome.freeterraforged.world.worldgen.noise.module.Noise;

// Three coastal noises are read at the position times a fixed factor of FreeTerraForged's own.
@Mixin(value = ArchipelagoPopulator.class, remap = false)
public abstract class ArchipelagoPopulatorMixin {
    @Unique
    private static final int COASTAL_MODULATION = 0;

    @Unique
    private static final int ANGULAR_COAST = 1;

    @Unique
    private static final int SHELF_DETAIL = 2;

    @Unique
    private static final float COASTAL_MODULATION_FACTOR = 0.005F;

    @Unique
    private static final float ANGULAR_COAST_FACTOR = 0.008F;

    @Unique
    private static final float SHELF_DETAIL_FACTOR = 0.02F;

    @WrapOperation(method = "apply",
            at = @At(value = "INVOKE", target = FreeTerraForgedInjectionTargets.NOISE_COMPUTE, ordinal = COASTAL_MODULATION))
    private float exotic_worlds$coastalModulationLap(Noise noise, float x, float z, int seed, Operation<Float> original) {
        return exotic_worlds$computeScaled(noise, x, z, seed, COASTAL_MODULATION_FACTOR, original);
    }

    // The angle added to the position is read off the island shape, which already closes on the lap.
    @WrapOperation(method = "apply",
            at = @At(value = "INVOKE", target = FreeTerraForgedInjectionTargets.NOISE_COMPUTE, ordinal = ANGULAR_COAST))
    private float exotic_worlds$angularCoastLap(Noise noise, float x, float z, int seed, Operation<Float> original) {
        return exotic_worlds$computeScaled(noise, x, z, seed, ANGULAR_COAST_FACTOR, original);
    }

    @WrapOperation(method = "apply",
            at = @At(value = "INVOKE", target = FreeTerraForgedInjectionTargets.NOISE_COMPUTE, ordinal = SHELF_DETAIL))
    private float exotic_worlds$shelfDetailLap(Noise noise, float x, float z, int seed, Operation<Float> original) {
        return exotic_worlds$computeScaled(noise, x, z, seed, SHELF_DETAIL_FACTOR, original);
    }

    @Unique
    private static float exotic_worlds$computeScaled(Noise noise, float x, float z, int seed, float factor,
            Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(noise, x, z, seed);
        }

        try (RtfLap.Frame.Scope scaled = frame.scale(factor, factor)) {
            return original.call(noise, x, z, seed);
        }
    }
}
