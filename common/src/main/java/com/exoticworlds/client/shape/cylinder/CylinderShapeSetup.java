package com.exoticworlds.client.shape.cylinder;

import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.shape.cylinder.CylinderShape;

import net.minecraft.client.gui.screens.Screen;

public final class CylinderShapeSetup {

    public static void register() {
        ShapeCustomizers.register(CylinderShape.MODULE.id(), CylinderShapeSetup::createScreen);
    }

    private static Screen createScreen(Screen parent) {
        return new CylinderSettingsScreen(parent, CylinderShape.MODULE.settings(), CylinderShape.MODULE::settings);
    }

    private CylinderShapeSetup() {
    }
}
