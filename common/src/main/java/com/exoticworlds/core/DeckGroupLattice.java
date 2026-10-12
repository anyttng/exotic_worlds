package com.exoticworlds.core;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

final class DeckGroupLattice {
    private static final int CANDIDATE_REACH = 2;

    private static final int COORD_FOLD_ATTEMPTS = 3;

    record Step(SeamTransform generator, boolean onX) {}

    final WrapDomain x;

    final WrapDomain z;

    private final Step[] steps;

    private final SeamTransform[] candidates;

    final @Nullable TranslationLattice translations;

    final DeckGroupRegions regions;

    DeckGroupLattice(FlatShape shape, int unit) {
        AxisBounds xBounds = shape.bounds().x();
        AxisBounds zBounds = shape.bounds().z();
        this.x = domainOf(xBounds, unit);
        this.z = domainOf(zBounds, unit);

        FlatShape.Mirror mirror = shape.mirror();
        boolean mirrorsZ = mirror != null && mirror.axis() == Direction.Axis.Z;
        boolean mirrorsX = mirror != null && mirror.axis() == Direction.Axis.X;
        int mirrorLine = mirror == null ? 0 : mirror.lineChunk() * unit;
        int skew = shape.skewChunks() * unit;

        Step xStep = xBounds instanceof AxisBounds.Looped
                ? new Step(mirrorsZ
                        ? SeamTransform.glideX(this.x.domainLength, mirrorShift(mirrorLine, this.z))
                        : SeamTransform.translation(this.x.domainLength, 0), true)
                : null;
        Step zStep = zBounds instanceof AxisBounds.Looped
                ? new Step(mirrorsX
                        ? SeamTransform.glideZ(mirrorShift(mirrorLine, this.x), this.z.domainLength)
                        : SeamTransform.translation(skew, this.z.domainLength), false)
                : null;

        this.steps = orderSteps(xStep, zStep, mirrorsZ);
        this.candidates = buildCandidates(this.steps);
        this.translations = mirror == null ? new TranslationLattice(this.x, this.z, skew) : null;
        this.regions = new DeckGroupRegions(this.x, this.z, this.steps);
    }

    private static int mirrorShift(int mirrorLine, WrapDomain mirrored) {
        int rawShift = 2 * mirrorLine;
        if (mirrored instanceof WrapDomain.Noop) {
            return rawShift;
        }

        int anchor = mirrored.lowerBound + mirrored.upperBound;
        int offset = Math.floorMod(rawShift - anchor, mirrored.domainLength);
        return anchor + (2 * offset > mirrored.domainLength ? offset - mirrored.domainLength : offset);
    }

    private static WrapDomain domainOf(AxisBounds axis, int unit) {
        return switch (axis) {
            case AxisBounds.Looped looped -> new WrapDomain(looped.minChunk() * unit, looped.maxChunk() * unit);
            case AxisBounds.Unbounded() -> new WrapDomain.Noop();
        };
    }

    private static Step[] orderSteps(Step xStep, Step zStep, boolean mirrorsZ) {
        if (xStep == null && zStep == null) {
            return new Step[0];
        }

        if (xStep == null) {
            return new Step[] {zStep};
        }

        if (zStep == null) {
            return new Step[] {xStep};
        }

        return mirrorsZ ? new Step[] {xStep, zStep} : new Step[] {zStep, xStep};
    }

    private static SeamTransform[] buildCandidates(Step[] steps) {
        if (steps.length == 0) {
            return new SeamTransform[] {SeamTransform.IDENTITY};
        }

        List<SeamTransform> built = new ArrayList<>();
        for (int first = -CANDIDATE_REACH; first <= CANDIDATE_REACH; first++) {
            SeamTransform firstPower = steps[0].generator().power(first);
            if (steps.length == 1) {
                built.add(firstPower);
                continue;
            }

            for (int second = -CANDIDATE_REACH; second <= CANDIDATE_REACH; second++) {
                built.add(firstPower.then(steps[1].generator().power(second)));
            }
        }

        return built.toArray(new SeamTransform[0]);
    }

    private int candidateCount() {
        return this.translations == null ? this.candidates.length : this.translations.nearCount();
    }

    private long nearFirst(double deltaX, double deltaZ) {
        return this.translations == null ? 0L : this.translations.nearFirst(deltaX, deltaZ);
    }

    private long nearSecond(double deltaX, double deltaZ) {
        return this.translations == null ? 0L : this.translations.nearSecond(deltaX, deltaZ);
    }

    private SeamTransform candidate(int index, long first, long second) {
        return this.translations == null
                ? this.candidates[index]
                : SeamTransform.translation(
                        candidateShiftX(index, first, second), candidateShiftZ(index, first, second));
    }

    private int candidateShiftX(int index, long first, long second) {
        return Math.toIntExact(-this.translations.nearShiftX(first, second, index));
    }

    private int candidateShiftZ(int index, long first, long second) {
        return Math.toIntExact(-this.translations.nearShiftZ(first, second, index));
    }

    private int candidateCellX(int index, long first, long second, int x) {
        return this.translations == null
                ? this.candidates[index].applyCellX(x)
                : x + candidateShiftX(index, first, second);
    }

    private int candidateCellZ(int index, long first, long second, int z) {
        return this.translations == null
                ? this.candidates[index].applyCellZ(z)
                : z + candidateShiftZ(index, first, second);
    }

    private double candidateX(int index, long first, long second, double x) {
        return this.translations == null
                ? this.candidates[index].applyX(x)
                : x + candidateShiftX(index, first, second);
    }

    private double candidateZ(int index, long first, long second, double z) {
        return this.translations == null
                ? this.candidates[index].applyZ(z)
                : z + candidateShiftZ(index, first, second);
    }

    SeamTransform foldCells(int cellX, int cellZ) {
        SeamTransform applied = SeamTransform.IDENTITY;
        int currentX = cellX;
        int currentZ = cellZ;
        for (Step step : this.steps) {
            WrapDomain domain = step.onX() ? this.x : this.z;
            int coord = step.onX() ? currentX : currentZ;
            long laps = Math.floorDiv((long) coord - domain.lowerBound, domain.domainLength);
            if (laps == 0) {
                continue;
            }

            SeamTransform move = step.generator().power(Math.toIntExact(-laps));
            int movedX = move.applyCellX(currentX);
            int movedZ = move.applyCellZ(currentZ);
            currentX = movedX;
            currentZ = movedZ;
            applied = applied.then(move);
        }

        return applied;
    }

    SeamTransform foldCoords(double coordX, double coordZ) {
        SeamTransform applied = SeamTransform.IDENTITY;
        double currentX = coordX;
        double currentZ = coordZ;
        for (Step step : this.steps) {
            WrapDomain domain = step.onX() ? this.x : this.z;
            for (int attempt = 0; attempt < COORD_FOLD_ATTEMPTS; attempt++) {
                double coord = step.onX() ? currentX : currentZ;
                long laps = (long) Math.floor((coord - domain.lowerBound) / domain.domainLength);
                if (laps == 0) {
                    break;
                }

                SeamTransform move = step.generator().power(Math.toIntExact(-laps));
                double movedX = move.applyX(currentX);
                double movedZ = move.applyZ(currentZ);
                currentX = movedX;
                currentZ = movedZ;
                applied = applied.then(move);
            }
        }

        return applied;
    }

    SeamTransform between(int fromX, int fromZ, int toX, int toZ) {
        return foldCells(fromX, fromZ).then(foldCells(toX, toZ).inverse());
    }

    SeamTransform nearestCells(int refX, int refZ, int targetX, int targetZ) {
        SeamTransform toRef = foldCells(refX, refZ);
        SeamTransform toTarget = foldCells(targetX, targetZ);
        SeamTransform fromRef = toRef.inverse();
        int foldedTargetX = toTarget.applyCellX(targetX);
        int foldedTargetZ = toTarget.applyCellZ(targetZ);

        int deltaX = foldedTargetX - toRef.applyCellX(refX);
        int deltaZ = foldedTargetZ - toRef.applyCellZ(refZ);
        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        int bestIndex = nearestCellCandidate(refX, refZ, targetX, targetZ, fromRef, first, second,
                foldedTargetX, foldedTargetZ);
        return bestIndex < 0
                ? SeamTransform.IDENTITY
                : toTarget.then(candidate(bestIndex, first, second)).then(fromRef);
    }

    long nearestCellsSqrDistance(int refX, int refZ, int targetX, int targetZ) {
        SeamTransform toRef = foldCells(refX, refZ);
        SeamTransform toTarget = foldCells(targetX, targetZ);
        SeamTransform fromRef = toRef.inverse();
        int foldedTargetX = toTarget.applyCellX(targetX);
        int foldedTargetZ = toTarget.applyCellZ(targetZ);

        int deltaX = foldedTargetX - toRef.applyCellX(refX);
        int deltaZ = foldedTargetZ - toRef.applyCellZ(refZ);
        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        int bestIndex = nearestCellCandidate(refX, refZ, targetX, targetZ, fromRef, first, second,
                foldedTargetX, foldedTargetZ);
        if (bestIndex < 0) {
            return squared((long) targetX - refX) + squared((long) targetZ - refZ);
        }

        long copyX = fromRef.applyCellX(candidateCellX(bestIndex, first, second, foldedTargetX));
        long copyZ = fromRef.applyCellZ(candidateCellZ(bestIndex, first, second, foldedTargetZ));
        return squared(copyX - refX) + squared(copyZ - refZ);
    }

    private int nearestCellCandidate(int refX, int refZ, int targetX, int targetZ, SeamTransform fromRef,
            long first, long second, int foldedTargetX, int foldedTargetZ) {
        int bestIndex = -1;
        long bestDistance = squared((long) targetX - refX) + squared((long) targetZ - refZ);
        long bestShiftX = 0;
        long bestShiftZ = 0;
        for (int index = 0; index < candidateCount(); index++) {
            int inFrameX = candidateCellX(index, first, second, foldedTargetX);
            int inFrameZ = candidateCellZ(index, first, second, foldedTargetZ);
            long copyX = fromRef.applyCellX(inFrameX);
            long copyZ = fromRef.applyCellZ(inFrameZ);
            long distance = squared(copyX - refX) + squared(copyZ - refZ);
            long shiftX = copyX - targetX;
            long shiftZ = copyZ - targetZ;
            if (distance < bestDistance || (distance == bestDistance
                    && closerToRest(shiftX, shiftZ, bestShiftX, bestShiftZ))) {
                bestIndex = index;
                bestDistance = distance;
                bestShiftX = shiftX;
                bestShiftZ = shiftZ;
            }
        }

        return bestIndex;
    }

    SeamTransform nearestCoords(double refX, double refZ, double targetX, double targetZ) {
        SeamTransform toRef = foldCoords(refX, refZ);
        SeamTransform toTarget = foldCoords(targetX, targetZ);
        SeamTransform fromRef = toRef.inverse();
        double foldedTargetX = toTarget.applyX(targetX);
        double foldedTargetZ = toTarget.applyZ(targetZ);

        double deltaX = foldedTargetX - toRef.applyX(refX);
        double deltaZ = foldedTargetZ - toRef.applyZ(refZ);
        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        int bestIndex = -1;
        double bestDistance = squared(targetX - refX) + squared(targetZ - refZ);
        double bestShiftX = 0.0;
        double bestShiftZ = 0.0;
        for (int index = 0; index < candidateCount(); index++) {
            double inFrameX = candidateX(index, first, second, foldedTargetX);
            double inFrameZ = candidateZ(index, first, second, foldedTargetZ);
            double copyX = fromRef.applyX(inFrameX);
            double copyZ = fromRef.applyZ(inFrameZ);
            double distance = squared(copyX - refX) + squared(copyZ - refZ);
            double shiftX = copyX - targetX;
            double shiftZ = copyZ - targetZ;
            if (distance < bestDistance || (distance == bestDistance
                    && closerToRest(shiftX, shiftZ, bestShiftX, bestShiftZ))) {
                bestIndex = index;
                bestDistance = distance;
                bestShiftX = shiftX;
                bestShiftZ = shiftZ;
            }
        }

        return bestIndex < 0
                ? SeamTransform.IDENTITY
                : toTarget.then(candidate(bestIndex, first, second)).then(fromRef);
    }

    double nearestBoxGap(double pointX, double pointZ, AABB box) {
        double centreX = SeamBoxes.centre(box.minX, box.maxX);
        double centreZ = SeamBoxes.centre(box.minZ, box.maxZ);
        SeamTransform toBox = foldCoords(centreX, centreZ);
        SeamTransform toPoint = foldCoords(pointX, pointZ);
        SeamTransform fromPoint = toPoint.inverse();

        double deltaX = toBox.applyX(centreX) - toPoint.applyX(pointX);
        double deltaZ = toBox.applyZ(centreZ) - toPoint.applyZ(pointZ);
        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        double best = gapSquared(box, pointX, pointZ);
        for (int index = 0; index < candidateCount(); index++) {
            SeamTransform candidate = candidate(index, first, second);
            best = Math.min(best, gapSquared(toBox.then(candidate).then(fromPoint).apply(box), pointX, pointZ));
        }

        return best;
    }

    private static double gapSquared(AABB box, double pointX, double pointZ) {
        double xGap = Math.max(Math.max(box.minX - pointX, pointX - box.maxX), 0.0);
        double zGap = Math.max(Math.max(box.minZ - pointZ, pointZ - box.maxZ), 0.0);
        return xGap * xGap + zGap * zGap;
    }

    private static boolean closerToRest(long firstX, long firstZ, long secondX, long secondZ) {
        long first = squared(firstX) + squared(firstZ);
        long second = squared(secondX) + squared(secondZ);
        if (first != second) {
            return first < second;
        }

        return firstX != secondX ? firstX < secondX : firstZ < secondZ;
    }

    private static boolean closerToRest(double firstX, double firstZ, double secondX, double secondZ) {
        double first = squared(firstX) + squared(firstZ);
        double second = squared(secondX) + squared(secondZ);
        if (first != second) {
            return first < second;
        }

        return firstX != secondX ? firstX < secondX : firstZ < secondZ;
    }

    private static long squared(long value) {
        return value * value;
    }

    private static double squared(double value) {
        return value * value;
    }
}
