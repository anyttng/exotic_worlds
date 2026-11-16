package com.exoticworlds.compat.distanthorizons;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.WorldFold;

public record DhLattice(ToroidalShape shape, int skewBlocks, String seamDistance) {
    public static DhLattice of(WorldFold fold) {
        ToroidalShape shape = fold.shapeView();
        int skewBlocks = fold.blockLattice().skew();
        return new DhLattice(shape, skewBlocks, DhSeamSql.seamDistance(shape, skewBlocks));
    }
}
