package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;

public final class SeamTarget extends DhBlockPos2D {
    private final ToroidalShape shape;

    private SeamTarget(ToroidalShape shape, DhBlockPos2D target) {
        super(target.x, target.z);
        this.shape = shape;
    }

    public static DhBlockPos2D of(@Nullable ToroidalShape shape, DhBlockPos2D target) {
        return shape == null ? target : new SeamTarget(shape, target);
    }

    public int chebyshevDistFrom(DhBlockPos2D pos) {
        return (int) DhFold.seamChebyshevDistance(this.shape, pos.x, pos.z, this.x, this.z);
    }
}
