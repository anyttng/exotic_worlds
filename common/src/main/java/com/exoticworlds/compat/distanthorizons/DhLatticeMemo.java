package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

public final class DhLatticeMemo {
    private volatile @Nullable Built built;

    @Nullable DhLattice latticeOf(@Nullable WorldFold fold) {
        if (fold == null) {
            return null;
        }

        Built current = this.built;
        if (current != null && current.fold() == fold) {
            return current.lattice();
        }

        DhLattice lattice = DhLattice.of(fold);
        DhProbes.keyPeriod(lattice, DhKeys.LEAF);
        this.built = new Built(fold, lattice);
        return lattice;
    }

    private record Built(WorldFold fold, DhLattice lattice) {
    }
}
