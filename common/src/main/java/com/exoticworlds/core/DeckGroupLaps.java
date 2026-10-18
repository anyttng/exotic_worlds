package com.exoticworlds.core;

final class DeckGroupLaps {
    static final long NONE = 0L;

    private static final int COORD_FOLD_ATTEMPTS = 3;

    private final WrapDomain x;

    private final WrapDomain z;

    private final DeckGroupLattice.Step[] steps;

    DeckGroupLaps(WrapDomain x, WrapDomain z, DeckGroupLattice.Step[] steps) {
        this.x = x;
        this.z = z;
        this.steps = steps;
    }

    long ofCells(int cellX, int cellZ) {
        int currentX = cellX;
        int currentZ = cellZ;
        int first = 0;
        int second = 0;
        for (int index = 0; index < this.steps.length; index++) {
            DeckGroupLattice.Step step = this.steps[index];
            WrapDomain domain = step.onX() ? this.x : this.z;
            int coord = step.onX() ? currentX : currentZ;
            long laps = Math.floorDiv((long) coord - domain.lowerBound, domain.domainLength);
            if (laps == 0) {
                continue;
            }

            int exponent = Math.toIntExact(-laps);
            int movedX = step.generator().powerApplyCellX(exponent, currentX);
            int movedZ = step.generator().powerApplyCellZ(exponent, currentZ);
            currentX = movedX;
            currentZ = movedZ;
            if (index == 0) {
                first = exponent;
            } else {
                second = exponent;
            }
        }

        return pack(first, second);
    }

    long ofCoords(double coordX, double coordZ) {
        double currentX = coordX;
        double currentZ = coordZ;
        int first = 0;
        int second = 0;
        for (int index = 0; index < this.steps.length; index++) {
            DeckGroupLattice.Step step = this.steps[index];
            WrapDomain domain = step.onX() ? this.x : this.z;
            int exponent = 0;
            for (int attempt = 0; attempt < COORD_FOLD_ATTEMPTS; attempt++) {
                double coord = step.onX() ? currentX : currentZ;
                long laps = (long) Math.floor((coord - domain.lowerBound) / domain.domainLength);
                if (laps == 0) {
                    break;
                }

                int move = Math.toIntExact(-laps);
                double movedX = step.generator().powerApplyX(move, currentX);
                double movedZ = step.generator().powerApplyZ(move, currentZ);
                currentX = movedX;
                currentZ = movedZ;
                exponent = Math.addExact(exponent, move);
            }

            if (index == 0) {
                first = exponent;
            } else {
                second = exponent;
            }
        }

        return pack(first, second);
    }

    private static long pack(int first, int second) {
        return ((long) first << Integer.SIZE) | Integer.toUnsignedLong(second);
    }

    private static int first(long laps) {
        return (int) (laps >> Integer.SIZE);
    }

    private static int second(long laps) {
        return (int) laps;
    }

    double foldX(long laps, double x) {
        return signX(laps) * x + shiftX(laps);
    }

    double foldZ(long laps, double z) {
        return signZ(laps) * z + shiftZ(laps);
    }

    int foldCellX(long laps, int x) {
        return SeamTransform.cellOf(signX(laps), shiftX(laps), x);
    }

    int foldCellZ(long laps, int z) {
        return SeamTransform.cellOf(signZ(laps), shiftZ(laps), z);
    }

    double unfoldX(long laps, double x) {
        int sign = signX(laps);
        return sign * x + -sign * shiftX(laps);
    }

    double unfoldZ(long laps, double z) {
        int sign = signZ(laps);
        return sign * z + -sign * shiftZ(laps);
    }

    int unfoldCellX(long laps, int x) {
        int sign = signX(laps);
        return SeamTransform.cellOf(sign, -sign * shiftX(laps), x);
    }

    int unfoldCellZ(long laps, int z) {
        int sign = signZ(laps);
        return SeamTransform.cellOf(sign, -sign * shiftZ(laps), z);
    }

    SeamTransform transformOf(long laps) {
        return laps == NONE
                ? SeamTransform.IDENTITY
                : new SeamTransform(signX(laps), signZ(laps), shiftX(laps), shiftZ(laps));
    }

    int signThroughX(long toTarget, int candidateSign, long toRef) {
        return signX(toTarget) * candidateSign * signX(toRef);
    }

    int signThroughZ(long toTarget, int candidateSign, long toRef) {
        return signZ(toTarget) * candidateSign * signZ(toRef);
    }

    int shiftThroughX(long toTarget, int candidateSign, int candidateShift, long toRef) {
        int refSign = signX(toRef);
        return refSign * (candidateSign * shiftX(toTarget) + candidateShift) + -refSign * shiftX(toRef);
    }

    int shiftThroughZ(long toTarget, int candidateSign, int candidateShift, long toRef) {
        int refSign = signZ(toRef);
        return refSign * (candidateSign * shiftZ(toTarget) + candidateShift) + -refSign * shiftZ(toRef);
    }

    private int signX(long laps) {
        return stepSignX(0, first(laps)) * stepSignX(1, second(laps));
    }

    private int signZ(long laps) {
        return stepSignZ(0, first(laps)) * stepSignZ(1, second(laps));
    }

    private int shiftX(long laps) {
        return stepSignX(1, second(laps)) * stepShiftX(0, first(laps)) + stepShiftX(1, second(laps));
    }

    private int shiftZ(long laps) {
        return stepSignZ(1, second(laps)) * stepShiftZ(0, first(laps)) + stepShiftZ(1, second(laps));
    }

    private int stepSignX(int index, int exponent) {
        return exponent == 0 ? 1 : this.steps[index].generator().powerSignX(exponent);
    }

    private int stepSignZ(int index, int exponent) {
        return exponent == 0 ? 1 : this.steps[index].generator().powerSignZ(exponent);
    }

    private int stepShiftX(int index, int exponent) {
        return exponent == 0 ? 0 : this.steps[index].generator().powerShiftX(exponent);
    }

    private int stepShiftZ(int index, int exponent) {
        return exponent == 0 ? 0 : this.steps[index].generator().powerShiftZ(exponent);
    }
}
