package com.exoticworlds.engine.seam;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.engine.fold.SeamDelta;

import net.minecraft.world.phys.Vec3;

public final class SpreadArea {
    public record Confined(double x, double z, boolean isClamped) {
    }

    public static boolean canFitInHalfTheWorld(WorldFold fold, double xSpanBlocks, double zSpanBlocks) {
        Vec3 diagonal = new Vec3(xSpanBlocks, 0.0, zSpanBlocks);
        Vec3 antiDiagonal = new Vec3(xSpanBlocks, 0.0, -zSpanBlocks);
        return SeamDelta.fold(fold, diagonal).equals(diagonal)
                && SeamDelta.fold(fold, antiDiagonal).equals(antiDiagonal);
    }

    public static double randomMin(WrapDomain axis, double min, double max) {
        return axis.isCoveringWorld(max - min) ? axis.lowerBound : min;
    }

    public static double randomMax(WrapDomain axis, double min, double max) {
        return axis.isCoveringWorld(max - min) ? axis.upperBound : max;
    }

    public static Confined confine(WorldFold fold, double x, double z,
            double minX, double minZ, double maxX, double maxZ) {
        TranslationLattice lattice = fold.getBlockLattice();
        boolean isXFree = lattice.getX().isCoveringWorld(maxX - minX);
        boolean isZFree = lattice.getZ().isCoveringWorld(maxZ - minZ);
        boolean isClamped = false;
        if (isZFree) {
            x -= (double) lattice.zLaps(z) * lattice.getSkew();
            z = lattice.foldZ(z);
        } else if (z < minZ) {
            z = minZ;
            isClamped = true;
        } else if (z > maxZ) {
            z = maxZ;
            isClamped = true;
        }

        if (isXFree) {
            x = lattice.getX().wrap(x);
        } else if (x < minX) {
            x = minX;
            isClamped = true;
        } else if (x > maxX) {
            x = maxX;
            isClamped = true;
        }

        return new Confined(x, z, isClamped);
    }

    private SpreadArea() {
    }
}
