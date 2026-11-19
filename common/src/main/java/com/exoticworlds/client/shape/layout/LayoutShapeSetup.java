package com.exoticworlds.client.shape.layout;

import com.ascendiacore.client.createworld.WorldTabRows;
import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.client.shape.WorldShapeRow;
import com.exoticworlds.shape.WorldShapes;
import com.exoticworlds.shape.layout.LayoutShape;
import com.exoticworlds.shape.layout.WorldLayout;

import net.minecraft.client.gui.screens.Screen;

public final class LayoutShapeSetup {

    public static void register() {
        ShapeCustomizers.register(LayoutShape.MODULE.id(), LayoutShapeSetup::createScreen);
        WorldTabRows.register(new WorldShapeRow(LayoutShape.MODULE.id()));
    }

    private static Screen createScreen(Screen parent) {
        return new WorldShapeSettingsScreen(parent, LayoutShape.MODULE.settings(), LayoutShapeSetup::choose);
    }

    private static void choose(WorldLayout layout) {
        LayoutShape.MODULE.settings(layout);
        WorldShapes.select(LayoutShape.MODULE.id());
    }

    private LayoutShapeSetup() {
    }
}
