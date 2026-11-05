package com.exoticworlds.compat.ultraterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.terraforged.SiteLattice;

public final class SiteSeats {
    public static SiteLattice.@Nullable Seat seat(float spacing, int cellX, int cellZ, float x, float z, int pointX,
            int pointZ) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null ? null : lattice.seat(cellX, cellZ, pointX, pointZ, x, z);
    }

    private SiteSeats() {
    }
}
