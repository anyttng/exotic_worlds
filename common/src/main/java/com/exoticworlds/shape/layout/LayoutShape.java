package com.exoticworlds.shape.layout;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.shape.ShapeModule;
import com.exoticworlds.client.shape.layout.LayoutShapeSetup;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.resources.Identifier;

public final class LayoutShape {
    private static final String LAYOUT_PATH = "layout";

    public static final ShapeModule<WorldLayout> MODULE = ShapeModule.of(
            Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, LAYOUT_PATH),
            WorldLayout.DEFAULT,
            LayoutDimensions::apply,
            LayoutDimensions::read);

    public static void register(boolean client) {
        MODULE.register();
        WorldShapes.takesSpawn(MODULE, WorldLayout::withSpawn);

        if (client) {
            LayoutShapeSetup.register();
        }
    }

    private LayoutShape() {
    }
}
