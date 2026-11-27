package com.exoticworlds.engine.fold;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WrapDomain;
import com.google.common.math.IntMath;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record LatticeBorder(TranslationLattice lattice, double minX, double maxX, double minZ, double maxZ)
        implements SeamBorder {
    @Override
    public boolean isInside(double x, double z, double marginBlocks) {
        WrapDomain xDomain = this.lattice.getX();
        WrapDomain zDomain = this.lattice.getZ();
        double dx = x - getCenterX();
        double dz = z - getCenterZ();
        if (xDomain.isCoveringWorld(getSpanX())) {
            return zDomain.isCoveringWorld(getSpanZ()) || isOffsetInside(zDomain.unwrapAround(0.0, dz), getHalfZ(), marginBlocks);
        }

        if (zDomain.isCoveringWorld(getSpanZ())) {
            return this.lattice.isSkewed()
                    ? isSomeSkewInside(xDomain.domainLength, dx, marginBlocks)
                    : isOffsetInside(xDomain.unwrapAround(0.0, dx), getHalfX(), marginBlocks);
        }

        if (!zDomain.isLooped()) {
            return isOffsetInside(dz, getHalfZ(), marginBlocks) && isOffsetInside(xDomain.unwrapAround(0.0, dx), getHalfX(), marginBlocks);
        }

        long firstLaps = (long) Math.floor((dz - getHalfZ() - marginBlocks) / zDomain.domainLength) + 1;
        long lastLaps = (long) Math.floor((dz + getHalfZ() + marginBlocks) / zDomain.domainLength);
        for (long laps = firstLaps; laps <= lastLaps; laps++) {
            if (isOffsetInside(dz - (double) laps * zDomain.domainLength, getHalfZ(), marginBlocks)
                    && isOffsetInside(xDomain.unwrapAround(0.0, dx - (double) laps * this.lattice.getSkew()), getHalfX(),
                    marginBlocks)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public double distanceToEdgeBlocks(double x, double z) {
        WrapDomain zDomain = this.lattice.getZ();
        double dx = x - getCenterX();
        double dz = z - getCenterZ();
        double best = Double.NEGATIVE_INFINITY;
        long first = this.lattice.nearFirst(dx, dz);
        long second = this.lattice.nearSecond(dx, dz);
        for (int index = 0; index < this.lattice.getNearCount(); index++) {
            long laps = this.lattice.nearZLap(first, second, index);
            double zOffsetBlocks = dz - (double) laps * zDomain.domainLength;
            double xOffsetBlocks = this.lattice.getX().unwrapAround(0.0, dx - (double) laps * this.lattice.getSkew());
            best = Math.max(best, Math.min(getHalfX() - Math.abs(xOffsetBlocks), getHalfZ() - Math.abs(zOffsetBlocks)));
        }

        return best;
    }

    @Override
    public Vec3 clamped(double x, double y, double z) {
        Vec3 box = nearestBoxCenter(x, z);
        return new Vec3(
                clampToBox(this.lattice.getX(), getSpanX(), box.x, getHalfX(), x),
                y,
                clampToBox(this.lattice.getZ(), getSpanZ(), box.z, getHalfZ(), z));
    }

    private Vec3 nearestBoxCenter(double x, double z) {
        WrapDomain xDomain = this.lattice.getX();
        WrapDomain zDomain = this.lattice.getZ();
        double boxX = getCenterX();
        double boxZ = getCenterZ();
        double bestGapBlocks = Double.NEGATIVE_INFINITY;
        long first = this.lattice.nearFirst(x - getCenterX(), z - getCenterZ());
        long second = this.lattice.nearSecond(x - getCenterX(), z - getCenterZ());
        for (int index = 0; index < this.lattice.getNearCount(); index++) {
            long laps = this.lattice.nearZLap(first, second, index);
            double copyZ = getCenterZ() + (double) laps * zDomain.domainLength;
            double copyX = xDomain.unwrapAround(x, getCenterX() + (double) laps * this.lattice.getSkew());
            double gapBlocks = Math.min(getHalfX() - Math.abs(x - copyX), getHalfZ() - Math.abs(z - copyZ));
            if (gapBlocks > bestGapBlocks) {
                bestGapBlocks = gapBlocks;
                boxX = copyX;
                boxZ = copyZ;
            }
        }

        return new Vec3(boxX, 0.0, boxZ);
    }

    public List<Vec3> getWallShifts() {
        List<Vec3> shifts = new ArrayList<>(9);
        for (int xLaps : neighborLaps(this.lattice.getX())) {
            for (int zLaps : neighborLaps(this.lattice.getZ())) {
                shifts.add(new Vec3(
                        (double) xLaps * this.lattice.getX().domainLength + (double) zLaps * this.lattice.getSkew(),
                        0.0,
                        (double) zLaps * this.lattice.getZ().domainLength));
            }
        }

        return shifts;
    }

    private boolean isSomeSkewInside(int widthBlocks, double dx, double marginBlocks) {
        int stepBlocks = IntMath.gcd(Math.abs(this.lattice.getSkew()), widthBlocks);
        double spanBlocks = 2.0 * (getHalfX() + marginBlocks);
        return spanBlocks >= stepBlocks || Mth.positiveModulo(dx + getHalfX() + marginBlocks, (double) stepBlocks) < spanBlocks;
    }

    private static int[] neighborLaps(WrapDomain domain) {
        return domain.isLooped() ? new int[] {-1, 0, 1} : new int[] {0};
    }

    private static boolean isOffsetInside(double offsetBlocks, double half, double marginBlocks) {
        return offsetBlocks >= -half - marginBlocks && offsetBlocks < half + marginBlocks;
    }

    private static double clampToBox(WrapDomain domain, double spanBlocks, double center, double half, double coord) {
        if (domain.isCoveringWorld(spanBlocks)) {
            return coord;
        }

        return Mth.clamp(coord, center - half, center + half - SeamBorder.CLAMP_INSET);
    }

    private double getCenterX() {
        return (this.minX + this.maxX) / 2.0;
    }

    private double getCenterZ() {
        return (this.minZ + this.maxZ) / 2.0;
    }

    private double getSpanX() {
        return this.maxX - this.minX;
    }

    private double getSpanZ() {
        return this.maxZ - this.minZ;
    }

    private double getHalfX() {
        return getSpanX() / 2.0;
    }

    private double getHalfZ() {
        return getSpanZ() / 2.0;
    }
}
