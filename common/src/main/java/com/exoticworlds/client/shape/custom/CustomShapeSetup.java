package com.exoticworlds.client.shape.custom;

import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.shape.custom.CustomShape;

import net.minecraft.client.gui.screens.Screen;

public final class CustomShapeSetup {

    public static void register() {
        ShapeCustomizers.register(CustomShape.MODULE.id(), CustomShapeSetup::createScreen);
    }

    private static Screen createScreen(Screen parent) {
        return new CustomSettingsScreen(parent, CustomShape.MODULE.settings(), CustomShape.MODULE::settings);
    }

    private CustomShapeSetup() {
    }
}
