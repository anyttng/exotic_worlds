package com.exoticworlds.client.shape.custom;

import java.util.Comparator;
import java.util.List;

import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.shape.custom.CustomShape;
import com.exoticworlds.shape.custom.ModDimension;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

public final class CustomShapeSetup {

    public static void register() {
        ShapeCustomizers.register(CustomShape.MODULE.id(), CustomShapeSetup::createScreen);
    }

    private static Screen createScreen(Screen parent) {
        return new CustomSettingsScreen(parent, CustomShape.MODULE.settings(), modDimensions(parent),
                CustomShape.MODULE::settings);
    }

    private static List<ModDimension> modDimensions(Screen parent) {
        if (!(parent instanceof CreateWorldScreen create)) {
            return List.of();
        }

        return create.getUiState().getSettings().datapackDimensions().registryKeySet().stream()
                .filter(key -> !DimensionTable.isVanillaStem(key))
                .sorted(Comparator.comparing(key -> key.identifier().toString()))
                .map(ModDimension::new)
                .toList();
    }

    private CustomShapeSetup() {
    }
}
