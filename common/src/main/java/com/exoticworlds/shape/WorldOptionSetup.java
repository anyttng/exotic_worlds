package com.exoticworlds.shape;

import java.util.List;

import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.cylinder.Cylinder;
import com.exoticworlds.shape.option.DimensionScope;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;
import com.exoticworlds.shape.feature.guaranteednethercomplexes.GuaranteedNetherComplexes;
import com.exoticworlds.shape.torus.Torus;

import net.minecraft.world.level.dimension.LevelStem;

public final class WorldOptionSetup {

    public static void registerAll() {
        registerSavedOptions();
        registerTorus();
        registerCylinder();
    }

    private static void registerSavedOptions() {
        ShapeOptionRegistry.register(SpawnPoint.OPTION);
        ShapeOptionRegistry.register(CompactBiomes.FEATURE.getOption());
        ShapeOptionRegistry.register(GuaranteedLand.FEATURE.getOption());
        ShapeOptionRegistry.register(GuaranteedNetherComplexes.FEATURE.getOption());
    }

    private static void registerTorus() {
        Torus.SHAPE.registerFeature(CompactBiomes.FEATURE, DimensionScope.ALL);
        Torus.SHAPE.registerFeature(GuaranteedLand.FEATURE, DimensionScope.of(List.of(LevelStem.OVERWORLD)));
        Torus.SHAPE.registerFeature(GuaranteedNetherComplexes.FEATURE, DimensionScope.of(List.of(LevelStem.NETHER)));
    }

    private static void registerCylinder() {
        Cylinder.SHAPE.registerFeature(CompactBiomes.FEATURE, DimensionScope.ALL);
    }

    private WorldOptionSetup() {
    }
}
