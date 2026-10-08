package com.exoticworlds.engine.fold;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WrapDomain;
import com.google.common.math.IntMath;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record SeamBorder(TranslationLattice lattice, double minX, double maxX, double minZ, double maxZ) {
    private static final double CLAMP_INSET = 1.0E-5F;

    public boolean inside(double x, double z, double margin) {
        WrapDomain xDomain = this.lattice.x();
        WrapDomain zDomain = this.lattice.z();
        double dx = x - centreX();
        double dz = z - centreZ();
        if (xDomain.coversWorld(spanX())) {
            return zDomain.coversWorld(spanZ()) || offsetInside(zDomain.unwrapAround(0.0, dz), halfZ(), margin);
        }

        if (zDomain.coversWorld(spanZ())) {
            return this.lattice.isSkewed()
                    ? someSkewInside(xDomain.domainLength, dx, margin)
                    : offsetInside(xDomain.unwrapAround(0.0, dx), halfX(), margin);
        }

        if (!zDomain.loops()) {
            return offsetInside(dz, halfZ(), margin) && offsetInside(xDomain.unwrapAround(0.0, dx), halfX(), margin);
        }

        long firstLaps = (long) Math.floor((dz - halfZ() - margin) / zDomain.domainLength) + 1;
        long lastLaps = (long) Math.floor((dz + halfZ() + margin) / zDomain.domainLength);
        for (long laps = firstLaps; laps <= lastLaps; laps++) {
            if (offsetInside(dz - (double) laps * zDomain.domainLength, halfZ(), margin)
                    && offsetInside(xDomain.unwrapAround(0.0, dx - (double) laps * this.lattice.skew()), halfX(),
                    margin)) {
                return true;
            }
        }

        return false;
    }

    public double distanceToEdge(double x, double z) {
        WrapDomain zDomain = this.lattice.z();
        double dx = x - centreX();
        double dz = z - centreZ();
        double nearestDz = zDomain.unwrapAround(0.0, dz);
        long nearestLaps = lapsOf(dz - nearestDz);
        double best = Double.NEGATIVE_INFINITY;
        for (long laps : lapsNearestFirst(nearestLaps)) {
            double zOffset = laps == nearestLaps ? nearestDz : dz - (double) laps * zDomain.domainLength;
            double xOffset = this.lattice.x().unwrapAround(0.0, dx - (double) laps * this.lattice.skew());
            best = Math.max(best, Math.min(halfX() - Math.abs(xOffset), halfZ() - Math.abs(zOffset)));
        }

        return best;
    }

    public Vec3 clamped(double x, double y, double z) {
        WrapDomain xDomain = this.lattice.x();
        WrapDomain zDomain = this.lattice.z();
        double nearestCentreZ = zDomain.unwrapAround(z, centreZ());
        long nearestLaps = lapsOf(nearestCentreZ - centreZ());
        double boxX = centreX();
        double boxZ = centreZ();
        double bestGap = Double.NEGATIVE_INFINITY;
        for (long laps : lapsNearestFirst(nearestLaps)) {
            double copyZ = laps == nearestLaps ? nearestCentreZ : centreZ() + (double) laps * zDomain.domainLength;
            double copyX = xDomain.unwrapAround(x, centreX() + (double) laps * this.lattice.skew());
            double gap = Math.min(halfX() - Math.abs(x - copyX), halfZ() - Math.abs(z - copyZ));
            if (gap > bestGap) {
                bestGap = gap;
                boxX = copyX;
                boxZ = copyZ;
            }
        }

        return new Vec3(
                clampToBox(xDomain, spanX(), boxX, halfX(), x),
                y,
                clampToBox(zDomain, spanZ(), boxZ, halfZ(), z));
    }

    public List<Vec3> wallShifts() {
        List<Vec3> shifts = new ArrayList<>(9);
        for (int xLaps : neighbourLaps(this.lattice.x())) {
            for (int zLaps : neighbourLaps(this.lattice.z())) {
                shifts.add(new Vec3(
                        (double) xLaps * this.lattice.x().domainLength + (double) zLaps * this.lattice.skew(),
                        0.0,
                        (double) zLaps * this.lattice.z().domainLength));
            }
        }

        return shifts;
    }

    private boolean someSkewInside(int width, double dx, double margin) {
        int step = IntMath.gcd(Math.abs(this.lattice.skew()), width);
        double span = 2.0 * (halfX() + margin);
        return span >= step || Mth.positiveModulo(dx + halfX() + margin, (double) step) < span;
    }

    private long lapsOf(double zShift) {
        return this.lattice.z().loops() ? Math.round(zShift / this.lattice.z().domainLength) : 0L;
    }

    private long[] lapsNearestFirst(long nearestLaps) {
        return this.lattice.z().loops()
                ? new long[] {nearestLaps, nearestLaps - 1, nearestLaps + 1}
                : new long[] {nearestLaps};
    }

    private static int[] neighbourLaps(WrapDomain domain) {
        return domain.loops() ? new int[] {-1, 0, 1} : new int[] {0};
    }

    private static boolean offsetInside(double offset, double half, double margin) {
        return offset >= -half - margin && offset < half + margin;
    }

    private static double clampToBox(WrapDomain domain, double span, double centre, double half, double coord) {
        if (domain.coversWorld(span)) {
            return coord;
        }

        return Mth.clamp(coord, centre - half, centre + half - CLAMP_INSET);
    }

    private double centreX() {
        return (this.minX + this.maxX) / 2.0;
    }

    private double centreZ() {
        return (this.minZ + this.maxZ) / 2.0;
    }

    private double spanX() {
        return this.maxX - this.minX;
    }

    private double spanZ() {
        return this.maxZ - this.minZ;
    }

    private double halfX() {
        return spanX() / 2.0;
    }

    private double halfZ() {
        return spanZ() / 2.0;
    }
}
