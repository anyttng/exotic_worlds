package com.exoticworlds.shape;

import java.util.List;
import java.util.Optional;

import com.exoticworlds.shape.feature.compactbiomes.client.CompactBiomesView;
import com.exoticworlds.shape.feature.spawnpoint.client.ChosenSpawnView;
import com.exoticworlds.core.registry.RegistrationBoundary;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.cuboid.Cuboid;
import com.exoticworlds.core.world.LegacyShapeIds;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.cylinder.Cylinder;
import com.exoticworlds.shape.dimension.AttachedFeatures;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.core.feature.DimensionScope;
import com.exoticworlds.shape.feature.floatingcrumbs.FloatingCrumbs;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;
import com.exoticworlds.shape.feature.guaranteednethercomplexes.GuaranteedNetherComplexes;
import com.exoticworlds.shape.feature.spawnpoint.ChosenSpawn;
import com.exoticworlds.shape.layout.LayoutShape;
import com.exoticworlds.shape.layout.client.LayoutShapeSetup;
import com.exoticworlds.shape.torus.Torus;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

public final class ShapeSetup {

    public static void registerAll(boolean isClient) {
        LegacyShapeIds.register(ShapeSetup::recognizeLegacy);
        registerFeatures();
        if (isClient) {
            registerFeatureViews();
        }

        registerTorus();
        registerCylinder();
        registerCuboid();
        registerLayout(isClient);
    }

    private static void registerFeatures() {
        ChosenSpawn.FEATURE.register();
        CompactBiomes.FEATURE.register();
        GuaranteedLand.FEATURE.register();
        GuaranteedNetherComplexes.FEATURE.register();
        FloatingCrumbs.FEATURE.register();
    }

    private static void registerFeatureViews() {
        ChosenSpawnView.register();
        CompactBiomesView.register();
    }

    static void registerLayout(boolean isClient) {
        LayoutShape.register();
        if (isClient) {
            LayoutShapeSetup.register();
        }

        WorldShapes.registerSpawnMove(LayoutShape.ID,
                spawn -> LayoutShape.trySetOverworldValue(ChosenSpawn.FEATURE, spawn));
        RegistrationBoundary.STARTUP.atFirstClose(DeclaredShapes::warnOfUnknownShapes);
    }

    private static void registerTorus() {
        DeclaredShapes.register(Torus.SHAPE);
        DeclaredShapes.registerPresetDefault(Torus.SHAPE);
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

    private static void registerCuboid() {
        DeclaredShapes.register(Cuboid.SHAPE);
        AttachedFeatures.attach(DeclaredShapes.CUBOID, FloatingCrumbs.FEATURE, DimensionScope.ALL);
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
