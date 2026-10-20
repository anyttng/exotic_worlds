package com.exoticworlds.engine.seam;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.fold.SeamDelta;

import net.minecraft.world.phys.Vec3;

public final class SpreadArea {
    public record Confined(double x, double z, boolean clamped) {
    }

    public static boolean fitsInHalfTheWorld(WorldFold fold, double xSpan, double zSpan) {
        Vec3 diagonal = new Vec3(xSpan, 0.0, zSpan);
        Vec3 antiDiagonal = new Vec3(xSpan, 0.0, -zSpan);
        return SeamDelta.fold(fold, diagonal).equals(diagonal)
                && SeamDelta.fold(fold, antiDiagonal).equals(antiDiagonal);
    }

    public static double randomMin(AxisBounds axis, double min, double max) {
        return axis.coversWorld(max - min) && axis instanceof AxisBounds.Looped looped ? looped.minBlock() : min;
    }

    public static double randomMax(AxisBounds axis, double min, double max) {
        return axis.coversWorld(max - min) && axis instanceof AxisBounds.Looped looped ? looped.maxBlock() : max;
    }

    public static Confined confine(WorldFold fold, double x, double z,
            double minX, double minZ, double maxX, double maxZ) {
        boolean freeX = fold.bounds().x().coversWorld(maxX - minX);
        boolean freeZ = fold.bounds().z().coversWorld(maxZ - minZ);
        TranslationLattice lattice = fold.blockLattice();
        boolean clamped = false;
        if (freeZ) {
            x -= (double) lattice.zLaps(z) * lattice.skew();
            z = lattice.foldZ(z);
        } else if (z < minZ) {
            z = minZ;
            clamped = true;
        } else if (z > maxZ) {
            z = maxZ;
            clamped = true;
        }

        if (freeX) {
            x = lattice.x().wrap(x);
        } else if (x < minX) {
            x = minX;
            clamped = true;
        } else if (x > maxX) {
            x = maxX;
            clamped = true;
        }

        return new Confined(x, z, clamped);
    }

    private SpreadArea() {
    }
}
