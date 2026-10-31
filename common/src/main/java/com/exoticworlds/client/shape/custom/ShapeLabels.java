package com.exoticworlds.client.shape.custom;

import java.util.Locale;

import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.WorldShapes;
import com.exoticworlds.shape.custom.CustomStem;
import com.exoticworlds.shape.custom.ModDimension;
import com.exoticworlds.shape.custom.StemKind;
import com.exoticworlds.shape.custom.VanillaDimension;
import com.exoticworlds.shape.cylinder.CylinderShape;
import com.exoticworlds.shape.torus.TorusShape;

import net.minecraft.network.chat.Component;

final class ShapeLabels {
    private static final String KEY_PREFIX = "gui.exotic_worlds.custom_settings.";
    private static final String FROM_OVERWORLD_KEY = KEY_PREFIX + "from_overworld";
    private static final String HINT_KEY_SUFFIX = ".hint";

    static Component dimension(CustomStem dimension) {
        return switch (dimension) {
            case VanillaDimension vanilla -> Component.translatable(KEY_PREFIX + vanilla.name().toLowerCase(Locale.ROOT));
            case ModDimension mod -> Component.literal(mod.stem().location().toString());
        };
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

    static Component choice(ModStemChoice choice) {
        StemKind kind = choice.kind();
        return kind == null ? Component.translatable(FROM_OVERWORLD_KEY) : kind(kind);
    }

    static Component choiceHint(ModStemChoice choice) {
        StemKind kind = choice.kind();
        return kind == null ? Component.translatable(FROM_OVERWORLD_KEY + HINT_KEY_SUFFIX) : kindHint(kind);
    }

    private ShapeLabels() {
    }
}
