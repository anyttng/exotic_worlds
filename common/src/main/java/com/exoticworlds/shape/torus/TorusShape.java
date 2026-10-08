package com.exoticworlds.shape.torus;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.client.shape.torus.TorusShapeSetup;
import com.exoticworlds.api.v1.shape.ShapeModule;

import net.minecraft.resources.Identifier;

public final class TorusShape {
    private static final String TORUS_PATH = "toroidal";

    public static final ShapeModule<TorusSettings> MODULE = ShapeModule.of(
            Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, TORUS_PATH),
            TorusSettings.DEFAULT,
            TorusDimensions::apply,
            TorusDimensions::read);

    public static void register(boolean client) {
        MODULE.register();

        if (client) {
            TorusShapeSetup.register();
        }
    }

    private TorusShape() {
    }
}
