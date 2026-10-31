package com.exoticworlds.client.shape.custom;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.shape.custom.DimensionShape;
import com.exoticworlds.shape.custom.StemKind;

enum ModStemChoice {
    FROM_OVERWORLD(null),
    NONE(StemKind.NONE),
    TORUS(StemKind.TORUS),
    CYLINDER(StemKind.CYLINDER);

    private final @Nullable StemKind kind;

    ModStemChoice(@Nullable StemKind kind) {
        this.kind = kind;
    }

    @Nullable StemKind kind() {
        return this.kind;
    }

    static ModStemChoice of(@Nullable DimensionShape shape) {
        if (shape == null) {
            return FROM_OVERWORLD;
        }

        return switch (shape.kind()) {
            case NONE -> NONE;
            case TORUS -> TORUS;
            case CYLINDER -> CYLINDER;
        };
    }
}
