package com.exoticworlds.shape.layout.client;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ascendiacore.client.createworld.WorldTabRows;
import com.exoticworlds.shape.client.WorldShapeRow;
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
        WorldLoopClientNetwork.openDimensionChoicesWith(DimensionChoicesScreens::open);
    }

    private static Screen createScreen(Screen parent) {
        if (!(parent instanceof CreateWorldScreen create)) {
            return WorldShapeSettingsScreen.forNewWorld(parent, LayoutShape.getLayout(), List.of(),
                    new CoordinateScales(Map.of()), LayoutShapeSetup::choose);
        }

        WorldCreationContext settings = create.getUiState().getSettings();
        CoordinateScales scales = scalesOf(settings);
        return WorldShapeSettingsScreen.forNewWorld(parent, LayoutShape.getLayout(),
                modDimensions(settings.datapackDimensions()), scales, LayoutShapeSetup::choose);
    }

    private static List<ResourceKey<LevelStem>> modDimensions(Registry<LevelStem> datapackDimensions) {
        return datapackDimensions.registryKeySet().stream()
                .filter(key -> !DimensionTable.isVanillaStem(key))
                .sorted(Comparator.comparing(key -> key.identifier().toString()))
                .toList();
    }

    private static CoordinateScales scalesOf(WorldCreationContext settings) {
        Map<ResourceKey<LevelStem>, Double> scales = new HashMap<>();
        settings.datapackDimensions().entrySet()
                .forEach(stem -> scales.put(stem.getKey(), getCoordinateScale(stem.getValue())));
        settings.selectedDimensions().dimensions()
                .forEach((key, stem) -> scales.put(key, getCoordinateScale(stem)));
        return new CoordinateScales(scales);
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
