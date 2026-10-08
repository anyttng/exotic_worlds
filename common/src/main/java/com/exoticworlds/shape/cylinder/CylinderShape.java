package com.exoticworlds.shape.cylinder;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.client.shape.cylinder.CylinderShapeSetup;
import com.exoticworlds.api.v1.shape.ShapeModule;

import net.minecraft.resources.Identifier;

public final class CylinderShape {
    private static final String CYLINDER_PATH = "cylinder";

    public static final ShapeModule<CylinderSettings> MODULE = ShapeModule.of(
            Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, CYLINDER_PATH),
            CylinderSettings.DEFAULT,
            CylinderDimensions::apply,
            CylinderDimensions::read);

    public static void register(boolean client) {
        MODULE.register();

        if (client) {
            CylinderShapeSetup.register();
        }
    }

    private CylinderShape() {
    }
}
