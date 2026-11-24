package com.exoticworlds.engine.noise.check;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;

enum SeamField {
    TEMPERATURE("temperature", true),
    HUMIDITY("humidity", true),
    CONTINENTALNESS("continentalness", true),
    EROSION("erosion", true),
    DEPTH("depth", true),
    WEIRDNESS("weirdness", true),
    COLUMN("column", false),
    BIOME("biome", false),
    SURFACE("surface", false),
    CARVERS("carvers", false),
    FEATURES("features", false);

    private final String id;
    private final boolean isClimate;

    SeamField(String id, boolean isClimate) {
        this.id = id;
        this.isClimate = isClimate;
    }

    String getId() {
        return this.id;
    }

    boolean isClimate() {
        return this.isClimate;
    }

    long getClimate(Climate.TargetPoint point) {
        return switch (this) {
            case TEMPERATURE -> point.temperature();
            case HUMIDITY -> point.humidity();
            case CONTINENTALNESS -> point.continentalness();
            case EROSION -> point.erosion();
            case DEPTH -> point.depth();
            case WEIRDNESS -> point.weirdness();
            case COLUMN, BIOME, SURFACE, CARVERS, FEATURES -> throw new IllegalStateException(this.id + " is no climate field");
        };
    }

    @Nullable DensityFunction getDensity(NoiseRouter router) {
        return switch (this) {
            case TEMPERATURE -> router.temperature();
            case HUMIDITY -> router.vegetation();
            case CONTINENTALNESS -> router.continents();
            case EROSION -> router.erosion();
            case DEPTH -> router.depth();
            case WEIRDNESS -> router.ridges();
            case COLUMN -> router.finalDensity();
            case BIOME, SURFACE, CARVERS, FEATURES -> null;
        };
    }
}
