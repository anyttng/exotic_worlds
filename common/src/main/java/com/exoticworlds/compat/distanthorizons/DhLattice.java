package com.exoticworlds.compat.distanthorizons;

import com.exoticworlds.core.fold.WorldFold;

public record DhLattice(WorldFold fold, int skewBlocks, String seamDistance) {
    public static DhLattice of(WorldFold fold) {
        int skewBlocks = fold.getBlockLattice().getSkew();
        return new DhLattice(fold, skewBlocks, DhSeamSql.seamDistance(fold, skewBlocks));
    }
}
