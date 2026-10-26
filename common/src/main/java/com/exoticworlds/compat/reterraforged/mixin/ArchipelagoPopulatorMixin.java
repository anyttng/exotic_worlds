package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.reterraforged.ReTerraForgedInjectionTargets;
import com.exoticworlds.compat.terraforged.RtfLap;

import raccoonman.reterraforged.world.worldgen.cell.terrain.populator.ArchipelagoPopulator;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

// The island mountains and volcanoes are read at the position times a frequency modifier of their own.
@Mixin(value = ArchipelagoPopulator.class, remap = false)
public abstract class ArchipelagoPopulatorMixin {
    @Unique
    private static final String CHANCE_MASK = "Lraccoonman/reterraforged/world/worldgen/cell/terrain/populator/"
            + "ArchipelagoPopulator;chanceMask(Lraccoonman/reterraforged/world/worldgen/noise/module/Noise;FFF)F";

    @Unique
    private static final int MOUNTAIN_MASK = 0;

    @Unique
    private static final int VOLCANO_MASK = 1;

    @Unique
    private static final int HILL_HEIGHT = 3;

    @Unique
    private static final int RIDGE_HEIGHT = 4;

    @Unique
    private static final int VOLCANO_HEIGHT = 5;

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = CHANCE_MASK, ordinal = MOUNTAIN_MASK))
    private float exotic_worlds$mountainMaskLap(Noise selector, float chance, float x, float z,
            Operation<Float> original, @Local(name = "mountainFreqMod") float modifier) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(selector, chance, x, z);
        }

        try (RtfLap.Frame.Scope scaled = frame.scale(modifier, modifier)) {
            return original.call(selector, chance, x, z);
        }
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = CHANCE_MASK, ordinal = VOLCANO_MASK))
    private float exotic_worlds$volcanoMaskLap(Noise selector, float chance, float x, float z,
            Operation<Float> original, @Local(name = "volcanoFreqMod") float modifier) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(selector, chance, x, z);
        }

        try (RtfLap.Frame.Scope scaled = frame.scale(modifier, modifier)) {
            return original.call(selector, chance, x, z);
        }
    }

    @WrapOperation(method = "apply", at = {
            @At(value = "INVOKE", target = ReTerraForgedInjectionTargets.NOISE_COMPUTE, ordinal = HILL_HEIGHT),
            @At(value = "INVOKE", target = ReTerraForgedInjectionTargets.NOISE_COMPUTE, ordinal = RIDGE_HEIGHT)})
    private float exotic_worlds$mountainLap(Noise noise, float x, float z, int seed, Operation<Float> original,
            @Local(name = "mountainFreqMod") float modifier) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(noise, x, z, seed);
        }

        try (RtfLap.Frame.Scope scaled = frame.scale(modifier, modifier)) {
            return original.call(noise, x, z, seed);
        }
    }

    @WrapOperation(method = "apply",
            at = @At(value = "INVOKE", target = ReTerraForgedInjectionTargets.NOISE_COMPUTE, ordinal = VOLCANO_HEIGHT))
    private float exotic_worlds$volcanoLap(Noise noise, float x, float z, int seed, Operation<Float> original,
            @Local(name = "volcanoFreqMod") float modifier) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(noise, x, z, seed);
        }

        try (RtfLap.Frame.Scope scaled = frame.scale(modifier, modifier)) {
            return original.call(noise, x, z, seed);
        }
    }
}
