package com.exoticworlds.compat.naturaltemperature;

public final class NaturalTemperatureInjectionTargets {
    public static final String PAINT = "modifyTemperature";

    public static final String TEMPERATURE_CALCULATION =
            "Lnet/minecraft/world/level/biome/Climate$Sampler;customTemperatureCalculation(III)F";

    public static final String HUMIDITY_CALCULATION =
            "Lnet/minecraft/world/level/biome/Climate$Sampler;customHumidityCalculation(III)F";

    public static final String LARGE_NOISE = "Lnaturaltemperature/NoiseBiomeBandsGenerator;getNoise(DDD)D";

    public static final String DETAIL_NOISE = "Lnaturaltemperature/NoiseBiomeBandsGenerator;getDetailNoise(DDD)D";

    public static final String NATURAL_WAVE = "Lnaturaltemperature/MathUtils;calculateNaturalWave(DDDJ)D";

    public static final String NEOFORGE_LARGE_NOISE =
            "Lcom/naturaltemperature/NoiseBiomeBandsGenerator;getNoise(DDD)D";

    public static final String NEOFORGE_DETAIL_NOISE =
            "Lcom/naturaltemperature/NoiseBiomeBandsGenerator;getDetailNoise(DDD)D";

    public static final String NEOFORGE_NATURAL_WAVE = "Lcom/naturaltemperature/MathUtils;calculateNaturalWave(DDDJ)D";

    private NaturalTemperatureInjectionTargets() {
    }
}
