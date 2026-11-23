package com.exoticworlds.api.v2.shape;

import net.minecraft.resources.Identifier;

/** A {@link DimensionShape} that holds its id, so a shape extending it states only its spans. */
public abstract class AbstractDimensionShape implements DimensionShape {
    private final Identifier id;

    protected AbstractDimensionShape(Identifier id) {
        this.id = id;
    }

    @Override
    public final Identifier id() {
        return this.id;
    }
}
