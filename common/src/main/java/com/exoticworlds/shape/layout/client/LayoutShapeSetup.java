package com.exoticworlds.shape.layout.client;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ascendiacore.client.createworld.WorldTabRows;
import com.exoticworlds.shape.client.WorldShapeRow;
import com.exoticworlds.core.gen.EndTerrain;
import com.exoticworlds.core.world.DimensionTable;
import com.exoticworlds.engine.client.WorldLoopClientNetwork;
import com.exoticworlds.shape.WorldShapes;
import com.exoticworlds.shape.layout.LayoutShape;
import com.exoticworlds.shape.layout.WorldLayout;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class LayoutShapeSetup {

    public static void register() {
        WorldTabRows.register(new WorldShapeRow(LayoutShapeSetup::createScreen));
        WorldLoopClientNetwork.setDimensionChoicesOpener(DimensionChoicesScreens::open);
    }

    private static Screen createScreen(Screen parent) {
        if (!(parent instanceof CreateWorldScreen create)) {
            return WorldShapeSettingsScreen.forNewWorld(parent, LayoutShape.getLayout(), List.of(),
                    StemFacts.NONE, LayoutShapeSetup::choose);
        }

        WorldCreationContext settings = create.getUiState().getSettings();
        return WorldShapeSettingsScreen.forNewWorld(parent, LayoutShape.getLayout(),
                modDimensions(settings.datapackDimensions()), factsOf(settings), LayoutShapeSetup::choose);
    }

    private static List<ResourceKey<LevelStem>> modDimensions(Registry<LevelStem> datapackDimensions) {
        return datapackDimensions.registryKeySet().stream()
                .filter(key -> !DimensionTable.isVanillaStem(key))
                .sorted(Comparator.comparing(key -> key.identifier().toString()))
                .toList();
    }

    private static StemFacts factsOf(WorldCreationContext settings) {
        Map<ResourceKey<LevelStem>, Double> scales = new HashMap<>();
        Set<ResourceKey<LevelStem>> endTerrain = new HashSet<>();
        settings.datapackDimensions().entrySet()
                .forEach(stem -> addFacts(stem.getKey(), stem.getValue(), scales, endTerrain));
        settings.selectedDimensions().dimensions()
                .forEach((key, stem) -> addFacts(key, stem, scales, endTerrain));
        return new StemFacts(scales, endTerrain);
    }

    private static void addFacts(ResourceKey<LevelStem> key, LevelStem stem, Map<ResourceKey<LevelStem>, Double> scales,
            Set<ResourceKey<LevelStem>> endTerrain) {
        scales.put(key, getCoordinateScale(stem));
        if (EndTerrain.isEndTerrain(key, stem.generator())) {
            endTerrain.add(key);
        }
    }

    private static double getCoordinateScale(LevelStem stem) {
        return stem.type().value().coordinateScale();
    }

    private static void choose(WorldLayout layout) {
        LayoutShape.choose(layout);
        WorldShapes.select(LayoutShape.ID);
    }

    private LayoutShapeSetup() {
    }
}
