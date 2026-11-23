package com.exoticworlds.compat.distanthorizons;

import com.exoticworlds.core.WorldFold;

public record DhLattice(WorldFold fold, int skewBlocks, String seamDistance) {
    public static DhLattice of(WorldFold fold) {
        int skewBlocks = fold.blockLattice().skew();
        return new DhLattice(fold, skewBlocks, DhSeamSql.seamDistance(fold, skewBlocks));
    }
}
