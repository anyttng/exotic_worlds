package com.exoticworlds.compat.ultraterraforged;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.terraforged.SiteLattice;

import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;

public final class SiteSeats {
    public static float hash(float spacing, Operation<Float> original, int seed, int cellX, int cellZ, int purpose,
            float x, float z, int pointX, int pointZ) {
        SiteLattice.Seat seat = seat(spacing, cellX, cellZ, x, z, pointX, pointZ);
        return seat == null
                ? original.call(seed, cellX, cellZ, purpose)
                : original.call(seed, seat.cellX(), seat.cellZ(), purpose);
    }

    public static Object site(float spacing, Operation<Object> original, SiteCache<?> cache, int cellX, int cellZ,
            SiteCache.Finder<?> finder, float x, float z, int pointX, int pointZ) {
        SiteLattice.Seat seat = seat(spacing, cellX, cellZ, x, z, pointX, pointZ);
        return seat == null
                ? original.call(cache, cellX, cellZ, finder)
                : original.call(cache, seat.cellX(), seat.cellZ(), finder);
    }

    public static SiteLattice.@Nullable Seat seat(float spacing, int cellX, int cellZ, float x, float z, int pointX,
            int pointZ) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null ? null : lattice.seat(cellX, cellZ, pointX, pointZ, x, z);
    }

    private SiteSeats() {
    }
}
