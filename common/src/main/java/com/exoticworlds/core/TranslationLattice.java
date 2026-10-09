package com.exoticworlds.core;

import java.util.Arrays;

public final class TranslationLattice {
    public static final int NO_SKEW = 0;

    private static final int[] NEIGHBOURS = {0, -1, 1};

    private static final long[] NO_SHIFT = {0L, 0L};

    private final WrapDomain x;

    private final WrapDomain z;

    private final int skew;

    private final int rank;

    private final long firstX;

    private final long firstZ;

    private final long secondX;

    private final long secondZ;

    public TranslationLattice(WrapDomain x, WrapDomain z, int skew) {
        if (skew != NO_SKEW && !(x.loops() && z.loops())) {
            throw new IllegalArgumentException("A skewed lattice needs both axes looped");
        }

        this.x = x;
        this.z = z;
        this.skew = skew;
        if (x.loops() && z.loops()) {
            long[] reduced = reducedBasis(x.domainLength, 0L, skew, z.domainLength);
            this.rank = 2;
            this.firstX = reduced[0];
            this.firstZ = reduced[1];
            this.secondX = reduced[2];
            this.secondZ = reduced[3];
        } else {
            this.rank = x.loops() || z.loops() ? 1 : 0;
            this.firstX = x.loops() ? x.domainLength : 0L;
            this.firstZ = z.loops() ? z.domainLength : 0L;
            this.secondX = 0L;
            this.secondZ = 0L;
        }
    }

    public static TranslationLattice unskewed(WrapDomain x, WrapDomain z) {
        return new TranslationLattice(x, z, NO_SKEW);
    }

    public WrapDomain x() {
        return this.x;
    }

    public WrapDomain z() {
        return this.z;
    }

    public int skew() {
        return this.skew;
    }

    public boolean isSkewed() {
        return this.skew != NO_SKEW;
    }

    public boolean bothLoop() {
        return this.x.loops() && this.z.loops();
    }

    public int zLaps(int coordZ) {
        return this.z.isOver(coordZ) ? Math.floorDiv(coordZ - this.z.lowerBound, this.z.domainLength) : 0;
    }

    public int zLaps(double coordZ) {
        int laps = this.z.lapsOver(coordZ);
        return laps != 0 && coordZ - (double) laps * this.z.domainLength >= this.z.upperBound ? laps + 1 : laps;
    }

    public int foldZ(int coordZ) {
        return this.z.wrap(coordZ);
    }

    public double foldZ(double coordZ) {
        return this.z.wrap(coordZ);
    }

    public int foldX(int coordX, int coordZ) {
        return this.x.wrap(this.skew == NO_SKEW ? coordX : coordX - zLaps(coordZ) * this.skew);
    }

    public double foldX(double coordX, double coordZ) {
        return this.x.wrap(this.skew == NO_SKEW ? coordX : coordX - (double) zLaps(coordZ) * this.skew);
    }

    public double rectangleX(double coordX, double coordZ) {
        return this.skew == NO_SKEW
                ? coordX
                : coordX - (coordZ - this.z.lowerBound) * this.skew / this.z.domainLength;
    }

    public long[] nearShifts(double deltaX, double deltaZ) {
        return switch (this.rank) {
            case 0 -> NO_SHIFT;
            case 1 -> nearShiftsOnLine(deltaX, deltaZ);
            default -> nearShiftsOnPlane(deltaX, deltaZ);
        };
    }

    public long[] zLapsNear(double deltaX, double deltaZ) {
        if (!this.z.loops()) {
            return new long[] {0L};
        }

        long[] shifts = nearShifts(deltaX, deltaZ);
        long[] laps = new long[shifts.length / 2];
        int count = 0;
        for (int index = 1; index < shifts.length; index += 2) {
            long lap = shifts[index] / this.z.domainLength;
            if (!contains(laps, count, lap)) {
                laps[count++] = lap;
            }
        }

        return Arrays.copyOf(laps, count);
    }

    public int nearestZLaps(int deltaX, int deltaZ) {
        if (this.skew == NO_SKEW) {
            int folded = this.z.unwrapAround(0, deltaZ);
            return this.z.loops() ? (deltaZ - folded) / this.z.domainLength : 0;
        }

        int nearest = 0;
        long nearestLength = Long.MAX_VALUE;
        for (long lap : zLapsNear(deltaX, deltaZ)) {
            long length = squaredLength(deltaX, deltaZ, Math.toIntExact(lap));
            if (length < nearestLength) {
                nearest = Math.toIntExact(lap);
                nearestLength = length;
            }
        }

        return nearest;
    }

    public int nearestDeltaX(int deltaX, int zLaps) {
        return this.x.unwrapAround(0, deltaX - zLaps * this.skew);
    }

    public int nearestDeltaZ(int deltaZ, int zLaps) {
        return deltaZ - zLaps * this.z.domainLength;
    }

    private long squaredLength(int deltaX, int deltaZ, int zLaps) {
        long reducedX = nearestDeltaX(deltaX, zLaps);
        long reducedZ = nearestDeltaZ(deltaZ, zLaps);
        return reducedX * reducedX + reducedZ * reducedZ;
    }

    private long[] nearShiftsOnLine(double deltaX, double deltaZ) {
        double along = (deltaX * this.firstX + deltaZ * this.firstZ)
                / ((double) this.firstX * this.firstX + (double) this.firstZ * this.firstZ);
        long centre = Math.round(along);
        long[] shifts = new long[NEIGHBOURS.length * 2];
        for (int index = 0; index < NEIGHBOURS.length; index++) {
            long times = centre + NEIGHBOURS[index];
            shifts[2 * index] = times * this.firstX;
            shifts[2 * index + 1] = times * this.firstZ;
        }

        return shifts;
    }

    private long[] nearShiftsOnPlane(double deltaX, double deltaZ) {
        double determinant = (double) this.firstX * this.secondZ - (double) this.firstZ * this.secondX;
        long firstCentre = Math.round((deltaX * this.secondZ - deltaZ * this.secondX) / determinant);
        long secondCentre = Math.round((deltaZ * this.firstX - deltaX * this.firstZ) / determinant);
        long[] shifts = new long[NEIGHBOURS.length * NEIGHBOURS.length * 2];
        int index = 0;
        for (int firstStep : NEIGHBOURS) {
            for (int secondStep : NEIGHBOURS) {
                long first = firstCentre + firstStep;
                long second = secondCentre + secondStep;
                shifts[index++] = first * this.firstX + second * this.secondX;
                shifts[index++] = first * this.firstZ + second * this.secondZ;
            }
        }

        return shifts;
    }

    private static long[] reducedBasis(long firstX, long firstZ, long secondX, long secondZ) {
        long ux = firstX;
        long uz = firstZ;
        long vx = secondX;
        long vz = secondZ;
        while (true) {
            if (ux * ux + uz * uz > vx * vx + vz * vz) {
                long swapX = ux;
                long swapZ = uz;
                ux = vx;
                uz = vz;
                vx = swapX;
                vz = swapZ;
            }

            long times = Math.round((double) (ux * vx + uz * vz) / (ux * ux + uz * uz));
            if (times == 0L) {
                return new long[] {ux, uz, vx, vz};
            }

            vx -= times * ux;
            vz -= times * uz;
        }
    }

    private static boolean contains(long[] values, int count, long value) {
        for (int index = 0; index < count; index++) {
            if (values[index] == value) {
                return true;
            }
        }

        return false;
    }
}
