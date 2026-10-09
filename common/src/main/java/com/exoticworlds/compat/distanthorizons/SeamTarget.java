package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;

public final class SeamTarget extends DhBlockPos2D {
    private final DhLattice lattice;

    private SeamTarget(DhLattice lattice, DhBlockPos2D target) {
        super(target.x, target.z);
        this.lattice = lattice;
    }

    public static DhBlockPos2D of(@Nullable DhLattice lattice, DhBlockPos2D target) {
        return lattice == null ? target : new SeamTarget(lattice, target);
    }

    public int chebyshevDistFrom(DhBlockPos2D pos) {
        return (int) DhFold.seamChebyshevDistance(this.lattice, pos.x, pos.z, this.x, this.z);
    }
}
