package com.exoticworlds.client.shape.layout;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ascendiacore.client.createworld.WorldTabRows;
import com.exoticworlds.api.v2.client.DimensionOptionLines;
import com.exoticworlds.client.shape.climate.CompactBiomesLine;
import com.exoticworlds.client.shape.WorldShapeRow;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.shape.WorldShapes;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.layout.LayoutShape;
import com.exoticworlds.shape.layout.ShapeRecognition;
import com.exoticworlds.shape.layout.WorldLayout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class LayoutShapeSetup {
    private static WeakReference<CreateWorldScreen> recognizedFor = new WeakReference<>(null);

    public static void register() {
        WorldTabRows.register(new WorldShapeRow(LayoutShapeSetup::createScreen));
        CompactBiomesLine factor = new CompactBiomesLine();
        DimensionOptionLines.register(CompactBiomes.SHOWN,
                (scale, spans) -> Minecraft.getInstance().screen instanceof WorldShapeSettingsScreen open
                        ? factor.line(open, open.creationScreen(), scale, spans)
                        : Component.empty());
    }

    private static Screen createScreen(Screen parent) {
        if (!(parent instanceof CreateWorldScreen create)) {
            return WorldShapeSettingsScreen.forNewWorld(parent, LayoutShape.layout(), List.of(),
                    new CoordinateScales(Map.of()), LayoutShapeSetup::choose);
        }

        WorldCreationContext settings = create.getUiState().getSettings();
        CoordinateScales scales = scalesOf(settings);
        if (recognizedFor.get() != create) {
            recognizedFor = new WeakReference<>(create);
            LayoutShape.choose(ShapeRecognition.recognize(LayoutShape.layout(), scales::of));
        }

        return WorldShapeSettingsScreen.forNewWorld(parent, LayoutShape.layout(),
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
                .forEach(stem -> scales.put(stem.getKey(), coordinateScale(stem.getValue())));
        settings.selectedDimensions().dimensions()
                .forEach((key, stem) -> scales.put(key, coordinateScale(stem)));
        return new CoordinateScales(scales);
    }

    private static double coordinateScale(LevelStem stem) {
        return stem.type().value().coordinateScale();
    }

    private static void choose(WorldLayout layout) {
        LayoutShape.choose(layout);
        WorldShapes.select(LayoutShape.ID);
    }

    private LayoutShapeSetup() {
    }
}
