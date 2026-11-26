package com.exoticworlds.shape;

import java.util.List;
import java.util.Optional;

import com.exoticworlds.shape.feature.compactbiomes.client.CompactBiomesView;
import com.exoticworlds.shape.feature.spawnpoint.client.ChosenSpawnView;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.registry.RegistrationBoundary;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.LegacyShapeIds;
import com.exoticworlds.shape.feature.OptionFeature;
import com.exoticworlds.shape.feature.ShapeFeature;
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
        registerFeatures();
        if (isClient) {
            registerFeatureViews();
        }

        registerTorus();
        registerCylinder();
        registerLayout(isClient);
    }

    private static void registerFeatures() {
        registerFeature(ChosenSpawn.FEATURE);
        registerFeature(CompactBiomes.FEATURE);
        registerFeature(GuaranteedLand.FEATURE);
        registerFeature(GuaranteedNetherComplexes.FEATURE);
        registerFeature(FloatingCrumbs.FEATURE);
    }

    private static void registerFeature(ShapeFeature feature) {
        if (feature instanceof OptionFeature<?> offered) {
            ShapeOptionRegistry.register(offered.getOption());
        }

        ShapeFeatures.register(feature);
    }

    private static void registerFeatureViews() {
        ChosenSpawnView.register();
        CompactBiomesView.register();
    }

    static void registerLayout(boolean isClient) {
        LayoutShape.register(isClient);
        WorldShapes.takesSpawn(LayoutShape.ID,
                spawn -> LayoutShape.trySetOverworldValue(ChosenSpawn.FEATURE, spawn));
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
