package com.exoticworlds.compat.ultraterraforged;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.terraforged.SiteLattice;

import com.pandaismyname1.ultraterraforged.world.worldgen.cave.CaveCarving;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;

public final class CaveCells {
    private static final ThreadLocal<@Nullable CopyCell> SEATED = new ThreadLocal<>();

    private record CopyCell(SiteLattice lattice, int cellX, int cellZ) {
    }

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

    public static int copyX(float spacing, int cellX, int cellZ, CaveCarving carving, int pointX) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null ? cellX : lattice.copyX(cellX, cellZ, pointX, carving.minX);
    }

    public static SiteCache.Finder<?> canonical(float spacing, SiteCache.Finder<?> finder) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null ? finder
                : (copyX, copyZ) -> finder.find(lattice.canonicalX(copyX), lattice.canonicalZ(copyZ));
    }

    public static void enter(float spacing, CaveCarving carving, int copyX, int copyZ) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        if (lattice != null) {
            ((CaveFrame) (Object) carving).exotic_worlds$shift(lattice.shiftX(copyX, copyZ), lattice.shiftZ(copyZ));
        }
    }

    public static void leave(CaveCarving carving) {
        ((CaveFrame) (Object) carving).exotic_worlds$shift(0, 0);
    }

    public static int frameX(CaveCarving carving, int x) {
        return x - ((CaveFrame) (Object) carving).exotic_worlds$shiftX();
    }

    public static int frameZ(CaveCarving carving, int z) {
        return z - ((CaveFrame) (Object) carving).exotic_worlds$shiftZ();
    }

    public static float frameX(CaveCarving carving, float x) {
        return x - ((CaveFrame) (Object) carving).exotic_worlds$shiftX();
    }

    public static float frameZ(CaveCarving carving, float z) {
        return z - ((CaveFrame) (Object) carving).exotic_worlds$shiftZ();
    }

    public static SiteCache.Finder<?> seated(float spacing, SiteCache.Finder<?> finder) {
        SiteLattice lattice = SiteLattice.bound(spacing);
        return lattice == null ? finder : (copyX, copyZ) -> find(lattice, finder, copyX, copyZ);
    }

    public static float seatX(float x) {
        CopyCell copy = SEATED.get();
        return copy == null ? x : copy.lattice().seatX(x, copy.cellX(), copy.cellZ());
    }

    public static float seatZ(float z) {
        CopyCell copy = SEATED.get();
        return copy == null ? z : copy.lattice().seatZ(z, copy.cellZ());
    }

    private static Object find(SiteLattice lattice, SiteCache.Finder<?> finder, int copyX, int copyZ) {
        CopyCell outer = SEATED.get();
        SEATED.set(new CopyCell(lattice, copyX, copyZ));
        try {
            return finder.find(lattice.canonicalX(copyX), lattice.canonicalZ(copyZ));
        } finally {
            SEATED.set(outer);
        }
    }

    private CaveCells() {
    }
}
