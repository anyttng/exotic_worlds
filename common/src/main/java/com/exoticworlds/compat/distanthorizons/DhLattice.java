package com.exoticworlds.compat.distanthorizons;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;

public record DhLattice(ToroidalShape shape, int skewBlocks) {
    public static DhLattice of(WorldFold fold) {
        return new DhLattice(new ToroidalShapeView(fold), fold.blockLattice().skew());
    }

    public boolean isSkewed() {
        return this.skewBlocks != 0;
    }
}
