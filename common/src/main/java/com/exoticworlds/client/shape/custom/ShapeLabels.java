package com.exoticworlds.client.shape.custom;

import java.util.Locale;

import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.WorldShapes;
import com.exoticworlds.shape.custom.StemKind;
import com.exoticworlds.shape.custom.VanillaDimension;
import com.exoticworlds.shape.cylinder.CylinderShape;
import com.exoticworlds.shape.torus.TorusShape;

import net.minecraft.network.chat.Component;

final class ShapeLabels {
    private static final String DIMENSION_KEY_PREFIX = "gui.exotic_worlds.custom_settings.";

    static Component dimension(VanillaDimension dimension) {
        return Component.translatable(DIMENSION_KEY_PREFIX + dimension.name().toLowerCase(Locale.ROOT));
    }

    static Component kind(StemKind kind) {
        return switch (kind) {
            case NONE -> WorldShapes.NORMAL.label();
            case TORUS -> WorldShape.label(TorusShape.MODULE.id());
            case CYLINDER -> WorldShape.label(CylinderShape.MODULE.id());
        };
    }

    static Component kindHint(StemKind kind) {
        return switch (kind) {
            case NONE -> WorldShapes.NORMAL.hint();
            case TORUS -> WorldShape.hint(TorusShape.MODULE.id());
            case CYLINDER -> WorldShape.hint(CylinderShape.MODULE.id());
        };
    }

    private ShapeLabels() {
    }
}
