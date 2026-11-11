package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;

enum SeamField {
    TEMPERATURE("temperature"),
    HUMIDITY("humidity"),
    CONTINENTALNESS("continentalness"),
    EROSION("erosion"),
    DEPTH("depth"),
    WEIRDNESS("weirdness"),
    COLUMN("column"),
    BIOME("biome");

    private final String id;

    SeamField(String id) {
        this.id = id;
    }

    String id() {
        return this.id;
    }

    boolean climate() {
        return this != COLUMN && this != BIOME;
    }

    long climateOf(Climate.TargetPoint point) {
        return switch (this) {
            case TEMPERATURE -> point.temperature();
            case HUMIDITY -> point.humidity();
            case CONTINENTALNESS -> point.continentalness();
            case EROSION -> point.erosion();
            case DEPTH -> point.depth();
            case WEIRDNESS -> point.weirdness();
            case COLUMN, BIOME -> throw new IllegalStateException(this.id + " is no climate field");
        };
    }

    @Nullable DensityFunction densityOf(NoiseRouter router) {
        return switch (this) {
            case TEMPERATURE -> router.temperature();
            case HUMIDITY -> router.vegetation();
            case CONTINENTALNESS -> router.continents();
            case EROSION -> router.erosion();
            case DEPTH -> router.depth();
            case WEIRDNESS -> router.ridges();
            case COLUMN -> router.finalDensity();
            case BIOME -> null;
        };
    }
}
