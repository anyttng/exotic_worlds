package com.exoticworlds.compat.naturaltemperature;

public final class NaturalTemperatureInjectionTargets {
    public static final String PAINT = "modifyTemperature";

    public static final String TEMPERATURE_CALCULATION =
            "Lnet/minecraft/world/level/biome/Climate$Sampler;customTemperatureCalculation(III)F";

    public static final String HUMIDITY_CALCULATION =
            "Lnet/minecraft/world/level/biome/Climate$Sampler;customHumidityCalculation(III)F";

    private NaturalTemperatureInjectionTargets() {
    }
}
