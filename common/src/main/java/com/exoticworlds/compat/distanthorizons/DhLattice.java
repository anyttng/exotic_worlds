package com.exoticworlds.compat.distanthorizons;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;

public record DhLattice(ToroidalShape shape, int skewBlocks, String seamDistance) {
    public static DhLattice of(WorldFold fold) {
        ToroidalShape shape = new ToroidalShapeView(fold);
        int skewBlocks = fold.blockLattice().skew();
        return new DhLattice(shape, skewBlocks, DhSeamSql.seamDistance(shape, skewBlocks));
    }

    public boolean isSkewed() {
        return this.skewBlocks != 0;
    }
}
