package com.exoticworlds.compat.simpleclouds;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;

public record CloudShape(ToroidalShape shape, int skewBlocks) {
    public static CloudShape of(WorldFold fold) {
        return new CloudShape(new ToroidalShapeView(fold), fold.blockLattice().skew());
    }
}
