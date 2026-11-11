package com.exoticworlds.shape.custom;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.shape.ShapeModule;
import com.exoticworlds.shape.WorldShapes;
import com.exoticworlds.client.shape.custom.CustomShapeSetup;

import net.minecraft.resources.ResourceLocation;

public final class CustomShape {
    private static final String CUSTOM_PATH = "custom";

    public static final ShapeModule<CustomSettings> MODULE = ShapeModule.of(
            ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, CUSTOM_PATH),
            CustomSettings.DEFAULT,
            CustomDimensions::apply,
            CustomDimensions::read);

    public static void register(boolean client) {
        MODULE.register();
        WorldShapes.takesSpawn(MODULE, CustomSettings::withSpawn);

        if (client) {
            CustomShapeSetup.register();
        }
    }

    private CustomShape() {
    }
}
