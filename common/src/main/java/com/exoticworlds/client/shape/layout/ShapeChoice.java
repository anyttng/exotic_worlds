package com.exoticworlds.client.shape.layout;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.network.chat.Component;

record ShapeChoice(@Nullable ShapeKind own, @Nullable DimensionShape declared) {
    private static final String DECLARED_HINT = "declared";

    static ShapeChoice of(ShapeKind own) {
        return new ShapeChoice(own, null);
    }

    static ShapeChoice of(DimensionShape declared) {
        return new ShapeChoice(null, declared);
    }

    Component getName() {
        return this.declared != null
                ? WorldShape.label(this.declared.getId())
                : SettingsLines.shapeName(this.getOwnOrNormal());
    }

    String getHintKey() {
        return this.declared != null ? DECLARED_HINT : this.getOwnOrNormal().name().toLowerCase(Locale.ROOT);
    }

    private ShapeKind getOwnOrNormal() {
        return this.own != null ? this.own : ShapeKind.NORMAL;
    }
}
