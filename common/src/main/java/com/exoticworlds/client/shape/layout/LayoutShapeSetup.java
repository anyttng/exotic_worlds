package com.exoticworlds.client.shape.layout;

import java.util.Comparator;
import java.util.List;

import com.ascendiacore.client.createworld.WorldTabRows;
import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.client.shape.WorldShapeRow;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.shape.WorldShapes;
import com.exoticworlds.shape.layout.LayoutShape;
import com.exoticworlds.shape.layout.WorldLayout;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.dimension.LevelStem;

public final class LayoutShapeSetup {
    private static final double NO_OVERWORLD_SCALE = 0.0;

    public static void register() {
        ShapeCustomizers.register(LayoutShape.MODULE.id(), LayoutShapeSetup::createScreen);
        WorldTabRows.register(new WorldShapeRow(LayoutShape.MODULE.id()));
    }

    private static Screen createScreen(Screen parent) {
        return WorldShapeSettingsScreen.forNewWorld(parent, LayoutShape.MODULE.settings(), modDimensions(parent),
                LayoutShapeSetup::choose);
    }

    private static List<ModStem> modDimensions(Screen parent) {
        if (!(parent instanceof CreateWorldScreen create)) {
            return List.of();
        }

        WorldCreationContext settings = create.getUiState().getSettings();
        double overworldScale = settings.selectedDimensions().get(LevelStem.OVERWORLD)
                .map(LayoutShapeSetup::coordinateScale)
                .orElse(NO_OVERWORLD_SCALE);
        return settings.datapackDimensions().entrySet().stream()
                .filter(stem -> !DimensionTable.isVanillaStem(stem.getKey()))
                .sorted(Comparator.comparing(stem -> stem.getKey().identifier().toString()))
                .map(stem -> new ModStem(stem.getKey(), overworldScale, coordinateScale(stem.getValue())))
                .toList();
    }

    private static double coordinateScale(LevelStem stem) {
        return stem.type().value().coordinateScale();
    }

    private static void choose(WorldLayout layout) {
        LayoutShape.MODULE.settings(layout);
        WorldShapes.select(LayoutShape.MODULE.id());
    }

    private LayoutShapeSetup() {
    }
}
