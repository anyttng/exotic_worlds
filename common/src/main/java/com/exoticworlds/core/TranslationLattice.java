package com.exoticworlds.core;

public final class TranslationLattice {
    public static final int NO_SKEW = 0;

    private static final int[] NEIGHBORS = {0, -1, 1};

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

    public TranslationLattice scaledDownByBits(int bits) {
        return new TranslationLattice(scaledDomain(this.x, bits), scaledDomain(this.z, bits), this.skew >> bits);
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

    public int rectangleX(int coordX, int coordZ) {
        return this.skew == NO_SKEW
                ? coordX
                : (int) (coordX - Math.floorDiv(((long) coordZ - this.z.lowerBound) * this.skew,
                        this.z.domainLength));
    }

    public int nearCount() {
        return switch (this.rank) {
            case 0 -> 1;
            case 1 -> NEIGHBORS.length;
            default -> NEIGHBORS.length * NEIGHBORS.length;
        };
    }

    public long nearFirst(double deltaX, double deltaZ) {
        return switch (this.rank) {
            case 0 -> 0L;
            case 1 -> Math.round((deltaX * this.firstX + deltaZ * this.firstZ)
                    / ((double) this.firstX * this.firstX + (double) this.firstZ * this.firstZ));
            default -> Math.round((deltaX * this.secondZ - deltaZ * this.secondX) / determinant());
        };
    }

    public long nearSecond(double deltaX, double deltaZ) {
        return this.rank < 2 ? 0L : Math.round((deltaZ * this.firstX - deltaX * this.firstZ) / determinant());
    }

    public long nearShiftX(long first, long second, int index) {
        return firstTimes(first, index) * this.firstX + secondTimes(second, index) * this.secondX;
    }

    public long nearShiftZ(long first, long second, int index) {
        return firstTimes(first, index) * this.firstZ + secondTimes(second, index) * this.secondZ;
    }

    public long nearZLap(long first, long second, int index) {
        return this.z.loops() ? nearShiftZ(first, second, index) / this.z.domainLength : 0L;
    }

    public int nearestZLaps(int deltaX, int deltaZ) {
        if (this.skew == NO_SKEW) {
            int folded = this.z.unwrapAround(0, deltaZ);
            return this.z.loops() ? (deltaZ - folded) / this.z.domainLength : 0;
        }

        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        int nearest = 0;
        long nearestLength = Long.MAX_VALUE;
        for (int index = 0; index < nearCount(); index++) {
            int lap = Math.toIntExact(nearZLap(first, second, index));
            long length = squaredLength(deltaX, deltaZ, lap);
            if (length < nearestLength) {
                nearest = lap;
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

    private long firstTimes(long first, int index) {
        return first + NEIGHBORS[this.rank == 2 ? index / NEIGHBORS.length : index];
    }

    private long secondTimes(long second, int index) {
        return this.rank == 2 ? second + NEIGHBORS[index % NEIGHBORS.length] : 0L;
    }

    private double determinant() {
        return (double) this.firstX * this.secondZ - (double) this.firstZ * this.secondX;
    }

    private static WrapDomain scaledDomain(WrapDomain domain, int bits) {
        return domain.loops() ? new WrapDomain(domain.lowerBound >> bits, domain.upperBound >> bits) : domain;
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
}
