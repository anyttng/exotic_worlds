package com.exoticworlds.compat.naturaltemperature;

public final class NaturalTemperatureInjectionTargets {
    public static final String LARGE_NOISE = "Lnaturaltemperature/NoiseBiomeBandsGenerator;getNoise(DDD)D";

    public static final String DETAIL_NOISE = "Lnaturaltemperature/NoiseBiomeBandsGenerator;getDetailNoise(DDD)D";

    public static final String NATURAL_WAVE = "Lnaturaltemperature/MathUtils;calculateNaturalWave(DDDJ)D";

    private NaturalTemperatureInjectionTargets() {
    }
}
