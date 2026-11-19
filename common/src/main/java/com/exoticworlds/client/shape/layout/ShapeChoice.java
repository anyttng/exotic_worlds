package com.exoticworlds.client.shape.layout;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.shape.DimensionShape;
import com.exoticworlds.shape.WorldShape;
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

    Component name() {
        return this.declared != null
                ? WorldShape.label(this.declared.id())
                : SettingsLines.shapeName(this.ownOrNormal());
    }

    String hintKey() {
        return this.declared != null ? DECLARED_HINT : this.ownOrNormal().name().toLowerCase(Locale.ROOT);
    }

    private ShapeKind ownOrNormal() {
        return this.own != null ? this.own : ShapeKind.NORMAL;
    }
}
