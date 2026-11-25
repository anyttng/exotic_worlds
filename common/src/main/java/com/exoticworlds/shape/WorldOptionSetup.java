package com.exoticworlds.shape;

import java.util.List;
import java.util.Optional;

import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.LegacyShapeIds;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.cylinder.Cylinder;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.option.DimensionScope;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;
import com.exoticworlds.shape.feature.guaranteednethercomplexes.GuaranteedNetherComplexes;
import com.exoticworlds.shape.torus.Torus;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

public final class WorldOptionSetup {

    public static void registerAll() {
        LegacyShapeIds.register(WorldOptionSetup::recognizeLegacy);
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
        DeclaredShapes.register(Torus.SHAPE);
        Torus.SHAPE.registerFeature(CompactBiomes.FEATURE, DimensionScope.ALL);
        Torus.SHAPE.registerFeature(GuaranteedLand.FEATURE, DimensionScope.of(List.of(LevelStem.OVERWORLD)));
        Torus.SHAPE.registerFeature(GuaranteedNetherComplexes.FEATURE, DimensionScope.of(List.of(LevelStem.NETHER)));
    }

    private static void registerCylinder() {
        DeclaredShapes.register(Cylinder.SHAPE);
        Cylinder.SHAPE.registerFeature(CompactBiomes.FEATURE, DimensionScope.ALL);
    }

    private static Optional<Identifier> recognizeLegacy(FlatShape spans) {
        return DeclaredShapes.getAll().stream()
                .filter(shape -> shape.optionsFor(spans).isPresent())
                .map(DimensionShape::getId)
                .findFirst();
    }

    private WorldOptionSetup() {
    }
}
