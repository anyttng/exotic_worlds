package com.exoticworlds.compat.freeterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.freeterraforged.FreeTerraForgedInjectionTargets;
import com.exoticworlds.compat.terraforged.RtfLap;

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
        try (RtfLap.Frame.Scope scaled = exotic_worlds$scaled(COASTAL_MODULATION_FACTOR)) {
            return original.call(noise, x, z, seed);
        }
    }

    // The angle added to the position is read off the island shape, which already closes on the lap.
    @WrapOperation(method = "apply",
            at = @At(value = "INVOKE", target = FreeTerraForgedInjectionTargets.NOISE_COMPUTE, ordinal = ANGULAR_COAST))
    private float exotic_worlds$angularCoastLap(Noise noise, float x, float z, int seed, Operation<Float> original) {
        try (RtfLap.Frame.Scope scaled = exotic_worlds$scaled(ANGULAR_COAST_FACTOR)) {
            return original.call(noise, x, z, seed);
        }
    }

    @WrapOperation(method = "apply",
            at = @At(value = "INVOKE", target = FreeTerraForgedInjectionTargets.NOISE_COMPUTE, ordinal = SHELF_DETAIL))
    private float exotic_worlds$shelfDetailLap(Noise noise, float x, float z, int seed, Operation<Float> original) {
        try (RtfLap.Frame.Scope scaled = exotic_worlds$scaled(SHELF_DETAIL_FACTOR)) {
            return original.call(noise, x, z, seed);
        }
    }

    @Unique
    private static RtfLap.Frame.@Nullable Scope exotic_worlds$scaled(float factor) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? null : frame.scale(factor, factor);
    }
}
