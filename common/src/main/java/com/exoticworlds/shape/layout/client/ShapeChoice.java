package com.exoticworlds.shape.layout.client;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.network.chat.Component;

record ShapeChoice(@Nullable ShapeKind mode, @Nullable DimensionShape shape) {
    static ShapeChoice of(ShapeKind mode) {
        return new ShapeChoice(mode, null);
    }

    static ShapeChoice of(DimensionShape shape) {
        return new ShapeChoice(null, shape);
    }

    Component getName() {
        return this.shape != null ? this.shape.getName() : SettingsLines.shapeName(this.getModeOrNormal());
    }

    Component getHint() {
        return this.shape != null ? this.shape.getHint() : SettingsLines.modeHint(this.getModeOrNormal());
    }

    private ShapeKind getModeOrNormal() {
        return this.mode != null ? this.mode : ShapeKind.NORMAL;
    }
}
