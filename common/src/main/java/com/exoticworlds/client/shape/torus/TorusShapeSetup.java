package com.exoticworlds.client.shape.torus;

import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.shape.torus.TorusShape;

import net.minecraft.client.gui.screens.Screen;

public final class TorusShapeSetup {

    public static void register() {
        ShapeCustomizers.register(TorusShape.MODULE.id(), TorusShapeSetup::createScreen);
    }

    private static Screen createScreen(Screen parent) {
        return new TorusSettingsScreen(parent, TorusShape.MODULE.settings(), TorusShape.MODULE::settings);
    }

    private TorusShapeSetup() {
    }
}
