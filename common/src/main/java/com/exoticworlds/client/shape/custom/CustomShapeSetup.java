package com.exoticworlds.client.shape.custom;

import com.ascendiacore.client.createworld.WorldTabRows;
import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.client.shape.WorldShapeRow;
import com.exoticworlds.shape.WorldShapes;
import com.exoticworlds.shape.custom.CustomSettings;
import com.exoticworlds.shape.custom.CustomShape;

import net.minecraft.client.gui.screens.Screen;

public final class CustomShapeSetup {

    public static void register() {
        ShapeCustomizers.register(CustomShape.MODULE.id(), CustomShapeSetup::createScreen);
        WorldTabRows.register(new WorldShapeRow(CustomShape.MODULE.id()));
    }

    private static Screen createScreen(Screen parent) {
        return new WorldShapeSettingsScreen(parent, CustomShape.MODULE.settings(), CustomShapeSetup::choose);
    }

    private static void choose(CustomSettings settings) {
        CustomShape.MODULE.settings(settings);
        WorldShapes.select(CustomShape.MODULE.id());
    }

    private CustomShapeSetup() {
    }
}
