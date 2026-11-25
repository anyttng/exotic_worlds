package com.exoticworlds.client.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.network.chat.Component;

record ShapeChoice(@Nullable ShapeKind own, @Nullable DimensionShape declared) {
    static ShapeChoice of(ShapeKind own) {
        return new ShapeChoice(own, null);
    }

    static ShapeChoice of(DimensionShape declared) {
        return new ShapeChoice(null, declared);
    }

    Component getName() {
        if (this.declared != null) {
            return this.declared.getName();
        }

        DimensionShape builtIn = this.getOwnOrNormal().getBuiltIn();
        return builtIn != null ? builtIn.getName() : SettingsLines.shapeName(this.getOwnOrNormal());
    }

    Component getHint() {
        if (this.declared != null) {
            return this.declared.getHint();
        }

        DimensionShape builtIn = this.getOwnOrNormal().getBuiltIn();
        return builtIn != null ? builtIn.getHint() : SettingsLines.modeHint(this.getOwnOrNormal());
    }

    private ShapeKind getOwnOrNormal() {
        return this.own != null ? this.own : ShapeKind.NORMAL;
    }
}
