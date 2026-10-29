package com.exoticworlds.compat.ultraterraforged;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.terraforged.SiteLattice;

import com.pandaismyname1.ultraterraforged.world.worldgen.cave.CaveCarving;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;

public final class CaveCells {
    public static int pointX(float spacing, int minX, int pointX) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null ? pointX : lattice.cellX(minX);
    }

    public static int pointZ(float spacing, int minZ, int pointZ) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null ? pointZ : lattice.cellZ(minZ);
    }

    public static float gate(float spacing, Operation<Float> original, int seed, int cellX, int cellZ, int purpose,
            CaveCarving carving, int pointX) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        if (lattice == null) {
            return original.call(seed, cellX, cellZ, purpose);
        }

        int copyX = lattice.copyX(cellX, cellZ, pointX, carving.minX);
        return original.call(seed, lattice.canonicalX(copyX), lattice.canonicalZ(cellZ), purpose);
    }

    public static Object site(float spacing, Operation<Object> original, SiteCache<?> cache, int cellX, int cellZ,
            SiteCache.Finder<?> finder, CaveCarving carving, int pointX) {
        return original.call(cache, copyX(spacing, cellX, cellZ, carving, pointX), cellZ, finder);
    }

    public static int copyX(float spacing, int cellX, int cellZ, CaveCarving carving, int pointX) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null ? cellX : lattice.copyX(cellX, cellZ, pointX, carving.minX);
    }

    public static float hash(float spacing, Operation<Float> original, int seed, int copyX, int copyZ, int purpose) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null
                ? original.call(seed, copyX, copyZ, purpose)
                : original.call(seed, lattice.canonicalX(copyX), lattice.canonicalZ(copyZ), purpose);
    }

    public static float originX(float spacing, float x, int copyZ) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null ? x : x + lattice.copySkew(copyZ);
    }

    private CaveCells() {
    }
}
