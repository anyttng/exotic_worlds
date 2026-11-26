package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.naturaltemperature.BandNoises;
import com.exoticworlds.compat.naturaltemperature.NaturalWaveCells;
import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.naturaltemperature.NoiseBiomeBandsGenerator;
import com.naturaltemperature.Painter.PainterCore;

import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

@Mixin(PainterCore.class)
public class PainterCoreMixin {
    private static final String TEMPERATURE = "calculateTemperature";

    private static final String HUMIDITY = "calculateHumidity";

    private static final String NATURAL_WAVE = "Lcom/naturaltemperature/MathUtils;calculateNaturalWave(DDDJ)D";

    private static final String WAVE_FREQUENCY = "waveFrequency";

    private static final String MAGNIFICATION = "magnification";

    @WrapOperation(
            method = {TEMPERATURE, HUMIDITY},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/naturaltemperature/NoiseBiomeBandsGenerator;getNoise(DDD)D"))
    private static double exotic_worlds$largeNoiseOnTheLap(NoiseBiomeBandsGenerator noise, double x, double z,
            double scale, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        if (sample == null) {
            return original.call(noise, x, z, scale);
        }

        return sample.getLap().noise(((BandNoises) noise).exotic_worlds$getLarge(), scale, sample.signedX(), z,
                sample.getBlockZ());
    }

    @WrapOperation(
            method = {TEMPERATURE, HUMIDITY},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/naturaltemperature/NoiseBiomeBandsGenerator;getDetailNoise(DDD)D"))
    private static double exotic_worlds$detailNoiseOnTheLap(NoiseBiomeBandsGenerator noise, double x, double z,
            double scale, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        if (sample == null) {
            return original.call(noise, x, z, scale);
        }

        return sample.getLap().noise(((BandNoises) noise).exotic_worlds$getDetail(), scale, sample.signedX(), z,
                sample.getBlockZ());
    }

    @WrapOperation(
            method = {TEMPERATURE, HUMIDITY},
            at = @At(value = "INVOKE", target = NATURAL_WAVE, ordinal = 0))
    private static double exotic_worlds$waveOnTheXLap(double x, double amplitude, double frequency, long seed,
            Operation<Double> original, @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        if (sample == null || !sample.getLap().isXWaveClosed()) {
            return original.call(x, amplitude, frequency, seed);
        }

        return sample.getLap().xWave(NaturalWaveCells.INSTANCE, seed, sample.signedX(), amplitude,
                waveFrequency / magnification);
    }

    @WrapOperation(method = HUMIDITY, at = @At(value = "INVOKE", target = NATURAL_WAVE, ordinal = 1))
    private static double exotic_worlds$waveOnTheZLap(double z, double amplitude, double frequency, long seed,
            Operation<Double> original, @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(name = WAVE_FREQUENCY) double waveFrequency, @Local(name = MAGNIFICATION) double magnification) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        if (sample == null || !sample.getLap().isZWaveClosed()) {
            return original.call(z, amplitude, frequency, seed);
        }

        return sample.getLap().zWave(NaturalWaveCells.INSTANCE, seed, sample.getBlockZ(), amplitude,
                waveFrequency / magnification);
    }

    @ModifyVariable(method = {TEMPERATURE, HUMIDITY}, at = @At(value = "LOAD", ordinal = 0), name = "wave")
    private static double exotic_worlds$foldAfterTheWave(double wave,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(argsOnly = true, ordinal = 2) int z, @Local(name = "distorsion") double distortion) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        return sample == null ? wave : sample.getLap().carriedWave(sample.getBlockZ(), z, wave, distortion);
    }

    @ModifyVariable(method = HUMIDITY, at = @At(value = "LOAD", ordinal = 0), name = "uwave")
    private static double exotic_worlds$bandsKeepTheirSide(double wave,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature,
            @Local(argsOnly = true, ordinal = 0) int x) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        return sample == null ? wave : sample.carriedSide(x, wave);
    }

    @ModifyVariable(method = HUMIDITY, at = @At("STORE"), name = "hbw")
    private static double exotic_worlds$bandsOnTheLap(double widthBlocks,
            @Local(argsOnly = true, ordinal = 0) DensitySampler.Bound rawTemperature) {
        PinnedSample sample = PinnedSample.of(rawTemperature);
        return sample == null ? widthBlocks : sample.getLap().lapBandWidthBlocks(widthBlocks);
    }
}
