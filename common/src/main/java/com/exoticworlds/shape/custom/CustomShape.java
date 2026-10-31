package com.exoticworlds.shape.custom;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.shape.ShapeModule;
import com.exoticworlds.client.shape.custom.CustomShapeSetup;

import net.minecraft.resources.Identifier;

public final class CustomShape {
    private static final String CUSTOM_PATH = "custom";

    public static final ShapeModule<CustomSettings> MODULE = ShapeModule.of(
            Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, CUSTOM_PATH),
            CustomSettings.DEFAULT,
            CustomDimensions::apply,
            CustomDimensions::read);

    public static void register(boolean client) {
        MODULE.register();

        if (client) {
            CustomShapeSetup.register();
        }
    }

    private CustomShape() {
    }
}
