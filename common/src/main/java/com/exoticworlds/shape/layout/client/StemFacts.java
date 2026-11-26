package com.exoticworlds.shape.layout.client;

import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.DerivedDimension;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeDerivations;
import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.shape.layout.DimensionLayout;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record StemFacts(Map<ResourceKey<LevelStem>, Double> coordinateScales,
        Set<ResourceKey<LevelStem>> endTerrainStems) {
    public static final StemFacts NONE = new StemFacts(Map.of(), Set.of());

    private static final double UNKNOWN = 0.0;

    public StemFacts {
        coordinateScales = Map.copyOf(coordinateScales);
        endTerrainStems = Set.copyOf(endTerrainStems);
    }

    @Nullable FlatShape modDimension(ResourceKey<LevelStem> dimension, DimensionLayout overworld) {
        return ShapeDerivations.derive(overworld.shape(), overworld.spans(), new DerivedDimension(dimension,
                this.terrainOf(dimension).isEndTerrain(), this.getCoordinateScale(LevelStem.OVERWORLD),
                this.getCoordinateScale(dimension)));
    }

    StemTerrain terrainOf(ResourceKey<LevelStem> dimension) {
        return new StemTerrain(dimension,
                StemTerrain.ofKey(dimension).isEndTerrain() || this.endTerrainStems.contains(dimension));
    }

    public double getCoordinateScale(ResourceKey<LevelStem> dimension) {
        return this.coordinateScales.getOrDefault(dimension, UNKNOWN);
    }
}
