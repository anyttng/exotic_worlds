package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.bawnorton.mixinsquared.TargetHandler;
import com.exoticworlds.compat.naturaltemperature.BandNoises;
import com.exoticworlds.compat.naturaltemperature.ClimateLap;
import com.exoticworlds.compat.naturaltemperature.NaturalTemperatureInjectionTargets;
import com.exoticworlds.compat.naturaltemperature.NaturalWaveCells;
import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import naturaltemperature.NoiseBiomeBandsGenerator;

import net.minecraft.world.level.biome.Climate;

@Mixin(value = Climate.Sampler.class, priority = 1100)
public class WorldPainterMixin {
    private static final String WORLD_PAINTER = "naturaltemperature.mixin.WorldPainter";

    private static final String PAINT = "modifyTemperature";

    private static final String HUMIDITY = "customHumidityCalculation(III)F";

    private static final String WAVE_FREQUENCY = "waveFrequency";

    private static final String MAGNIFICATION = "magnification";

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Climate$Sampler;customTemperatureCalculation(III)F"))
    @TargetHandler(mixin = WORLD_PAINTER, name = PAINT)
    private float exotic_worlds$paintTheLapTemperature(Climate.Sampler sampler, int x, int y, int z,
            Operation<Float> original) {
        ClimateLap lap = ClimateLap.bound();
        if (lap == null) {
            return original.call(sampler, x, y, z);
        }

        PinnedSample previous = PinnedSample.enter(lap, x, y, z);
        try {
            return original.call(sampler, lap.x(x, z), y, lap.z(z));
        } finally {
            PinnedSample.exit(previous);
        }
    }

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Climate$Sampler;customHumidityCalculation(III)F"))
    @TargetHandler(mixin = WORLD_PAINTER, name = PAINT)
    private float exotic_worlds$paintTheLapHumidity(Climate.Sampler sampler, int x, int y, int z,
            Operation<Float> original) {
        ClimateLap lap = ClimateLap.bound();
        if (lap == null) {
            return original.call(sampler, x, y, z);
        }

        PinnedSample previous = PinnedSample.enter(lap, x, y, z);
        try {
            return original.call(sampler, lap.x(x, z), y, lap.z(z));
        } finally {
            PinnedSample.exit(previous);
        }
    }

    @WrapOperation(
            method = HUMIDITY,
            at = @At(value = "INVOKE", target = NaturalTemperatureInjectionTargets.LARGE_NOISE))
    private double exotic_worlds$largeNoiseOnTheLap(NoiseBiomeBandsGenerator noise, double x, double z, double scale,
            Operation<Double> original) {
        PinnedSample sample = PinnedSample.current();
        if (sample == null) {
            return original.call(noise, x, z, scale);
        }

        return sample.lap().noise(((BandNoises) noise).exotic_worlds$large(), scale, sample.signedX(), z,
                sample.blockZ());
    }

    @WrapOperation(
            method = HUMIDITY,
            at = @At(value = "INVOKE", target = NaturalTemperatureInjectionTargets.DETAIL_NOISE))
    private double exotic_worlds$detailNoiseOnTheLap(NoiseBiomeBandsGenerator noise, double x, double z,
            double scale, Operation<Double> original) {
        PinnedSample sample = PinnedSample.current();
        if (sample == null) {
            return original.call(noise, x, z, scale);
        }

        return sample.lap().noise(((BandNoises) noise).exotic_worlds$detail(), scale, sample.signedX(), z,
                sample.blockZ());
    }

    @WrapOperation(
            method = HUMIDITY,
            at = @At(value = "INVOKE", target = NaturalTemperatureInjectionTargets.NATURAL_WAVE, ordinal = 0))
    private double exotic_worlds$waveOnTheXLap(double x, double amplitude, double frequency, long seed,
            Operation<Double> original, @Local(name = WAVE_FREQUENCY) double waveFrequency,
            @Local(name = MAGNIFICATION) double magnification) {
        PinnedSample sample = PinnedSample.current();
        if (sample == null || !sample.lap().closesXWave()) {
            return original.call(x, amplitude, frequency, seed);
        }

        return sample.lap().xWave(NaturalWaveCells.INSTANCE, seed, sample.signedX(), amplitude,
                waveFrequency / magnification);
    }

    @WrapOperation(
            method = HUMIDITY,
            at = @At(value = "INVOKE", target = NaturalTemperatureInjectionTargets.NATURAL_WAVE, ordinal = 1))
    private double exotic_worlds$waveOnTheZLap(double z, double amplitude, double frequency, long seed,
            Operation<Double> original, @Local(name = WAVE_FREQUENCY) double waveFrequency,
            @Local(name = MAGNIFICATION) double magnification) {
        PinnedSample sample = PinnedSample.current();
        if (sample == null || !sample.lap().closesZWave()) {
            return original.call(z, amplitude, frequency, seed);
        }

        return sample.lap().zWave(NaturalWaveCells.INSTANCE, seed, sample.blockZ(), amplitude,
                waveFrequency / magnification);
    }

    @ModifyVariable(method = HUMIDITY, at = @At(value = "LOAD", ordinal = 0), name = "wave")
    private double exotic_worlds$foldAfterTheWave(double wave, @Local(argsOnly = true, ordinal = 2) int z,
            @Local(name = "distorsion") double distortion) {
        PinnedSample sample = PinnedSample.current();
        return sample == null ? wave : sample.lap().carriedWave(sample.blockZ(), z, wave, distortion);
    }

    @ModifyVariable(method = HUMIDITY, at = @At(value = "LOAD", ordinal = 0), name = "uwave")
    private double exotic_worlds$bandsKeepTheirSide(double wave, @Local(argsOnly = true, ordinal = 0) int x) {
        PinnedSample sample = PinnedSample.current();
        return sample == null ? wave : sample.carriedSide(x, wave);
    }

    @ModifyVariable(method = HUMIDITY, at = @At("STORE"), name = "hbw")
    private double exotic_worlds$bandsOnTheLap(double width) {
        PinnedSample sample = PinnedSample.current();
        return sample == null ? width : sample.lap().lapBandWidth(width);
    }
}
