package com.exoticworlds.core;

public record TranslationLattice(WrapDomain x, WrapDomain z, int skew) {
    public static final int NO_SKEW = 0;

    private static final int[] NEIGHBOUR_LAPS = {-1, 1};

    public TranslationLattice {
        if (skew != NO_SKEW && !(x.loops() && z.loops())) {
            throw new IllegalArgumentException("A skewed lattice needs both axes looped");
        }
    }

    public static TranslationLattice unskewed(WrapDomain x, WrapDomain z) {
        return new TranslationLattice(x, z, NO_SKEW);
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

    public int nearestZLaps(int deltaX, int deltaZ) {
        int folded = this.z.unwrapAround(0, deltaZ);
        int laps = this.z.loops() ? (deltaZ - folded) / this.z.domainLength : 0;
        if (this.skew == NO_SKEW) {
            return laps;
        }

        int nearest = laps;
        long nearestLength = squaredLength(deltaX, deltaZ, laps);
        for (int neighbour : NEIGHBOUR_LAPS) {
            long length = squaredLength(deltaX, deltaZ, laps + neighbour);
            if (length < nearestLength) {
                nearest = laps + neighbour;
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
}
