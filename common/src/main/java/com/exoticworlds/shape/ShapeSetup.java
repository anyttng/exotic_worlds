package com.exoticworlds.shape;

import java.util.List;
import java.util.Optional;

import com.exoticworlds.shape.feature.compactbiomes.client.CompactBiomesView;
import com.exoticworlds.shape.feature.spawnpoint.client.ChosenSpawnView;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.registry.RegistrationBoundary;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.LegacyShapeIds;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.cylinder.Cylinder;
import com.exoticworlds.shape.dimension.AttachedFeatures;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.core.feature.DimensionScope;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.shape.feature.floatingcrumbs.FloatingCrumbs;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;
import com.exoticworlds.shape.feature.guaranteednethercomplexes.GuaranteedNetherComplexes;
import com.exoticworlds.shape.feature.spawnpoint.ChosenSpawn;
import com.exoticworlds.shape.layout.LayoutShape;
import com.exoticworlds.shape.torus.Torus;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

public final class ShapeSetup {

    public static void registerAll(boolean isClient) {
        LegacyShapeIds.register(ShapeSetup::recognizeLegacy);
        registerSavedOptions();
        registerFeatures();
        if (isClient) {
            registerFeatureViews();
        }

        registerTorus();
        registerCylinder();
        registerLayout(isClient);
    }

    private static void registerSavedOptions() {
        ShapeOptionRegistry.register(SpawnPoint.OPTION);
        ShapeOptionRegistry.register(CompactBiomes.FEATURE.getOption());
        ShapeOptionRegistry.register(GuaranteedLand.FEATURE.getOption());
        ShapeOptionRegistry.register(GuaranteedNetherComplexes.FEATURE.getOption());
    }

    private static void registerFeatures() {
        ShapeFeatures.register(ChosenSpawn.FEATURE);
        ShapeFeatures.register(CompactBiomes.FEATURE);
        ShapeFeatures.register(GuaranteedLand.FEATURE);
        ShapeFeatures.register(GuaranteedNetherComplexes.FEATURE);
        ShapeFeatures.register(FloatingCrumbs.FEATURE);
    }

    private static void registerFeatureViews() {
        ChosenSpawnView.register();
        CompactBiomesView.register();
    }

    private static void registerLayout(boolean isClient) {
        LayoutShape.register(isClient);
        RegistrationBoundary.STARTUP.atFirstClose(DeclaredShapes::warnOfUnknownShapes);
    }

    private static void registerTorus() {
        DeclaredShapes.register(Torus.SHAPE);
        AttachedFeatures.attach(DeclaredShapes.TORUS, ChosenSpawn.FEATURE, DimensionScope.of(List.of(LevelStem.OVERWORLD)));
        AttachedFeatures.attach(DeclaredShapes.TORUS, CompactBiomes.FEATURE, DimensionScope.ALL);
        AttachedFeatures.attach(DeclaredShapes.TORUS, GuaranteedLand.FEATURE, DimensionScope.of(List.of(LevelStem.OVERWORLD)));
        AttachedFeatures.attach(DeclaredShapes.TORUS, GuaranteedNetherComplexes.FEATURE, DimensionScope.of(List.of(LevelStem.NETHER)));
        AttachedFeatures.attach(DeclaredShapes.TORUS, FloatingCrumbs.FEATURE, DimensionScope.ALL);
    }

    private static void registerCylinder() {
        DeclaredShapes.register(Cylinder.SHAPE);
        AttachedFeatures.attach(DeclaredShapes.CYLINDER, ChosenSpawn.FEATURE, DimensionScope.of(List.of(LevelStem.OVERWORLD)));
        AttachedFeatures.attach(DeclaredShapes.CYLINDER, CompactBiomes.FEATURE, DimensionScope.ALL);
        AttachedFeatures.attach(DeclaredShapes.CYLINDER, FloatingCrumbs.FEATURE, DimensionScope.ALL);
    }

    private static Optional<Identifier> recognizeLegacy(FlatShape spans) {
        return DeclaredShapes.getAll().stream()
                .filter(shape -> shape.optionsFor(spans).isPresent())
                .map(DimensionShape::getId)
                .findFirst();
    }

    private ShapeSetup() {
    }
}
