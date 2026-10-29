package com.exoticworlds.core;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

final class DeckGroupLattice {
    private static final int CANDIDATE_REACH = 2;

    record Step(SeamTransform generator, boolean onX) {}

    final WrapDomain x;

    final WrapDomain z;

    private final Step[] steps;

    private final SeamTransform[] candidates;

    final @Nullable TranslationLattice translations;

    final DeckGroupRegions regions;

    final DeckGroupLaps laps;

    DeckGroupLattice(FlatShape shape, int unit) {
        this.x = domainOf(shape.bounds().x(), unit);
        this.z = domainOf(shape.bounds().z(), unit);

        int skew = shape.skewChunks() * unit;
        this.steps = orderSteps(stepAlongX(shape, unit), stepAlongZ(shape, unit, skew),
                mirrors(shape.mirror(), Direction.Axis.Z));
        this.candidates = buildCandidates(this.steps);
        this.laps = new DeckGroupLaps(this.x, this.z, this.steps);
        this.translations = shape.mirror() == null ? new TranslationLattice(this.x, this.z, skew) : null;
        this.regions = new DeckGroupRegions(this.x, this.z, this.steps);
    }

    private @Nullable Step stepAlongX(FlatShape shape, int unit) {
        if (!(shape.bounds().x() instanceof AxisBounds.Looped)) {
            return null;
        }

        SeamTransform generator = mirrors(shape.mirror(), Direction.Axis.Z)
                ? SeamTransform.glideX(this.x.domainLength, mirrorShift(mirrorLine(shape.mirror(), unit), this.z))
                : SeamTransform.translation(this.x.domainLength, 0);
        return new Step(generator, true);
    }

    private @Nullable Step stepAlongZ(FlatShape shape, int unit, int skew) {
        if (!(shape.bounds().z() instanceof AxisBounds.Looped)) {
            return null;
        }

        SeamTransform generator = mirrors(shape.mirror(), Direction.Axis.X)
                ? SeamTransform.glideZ(mirrorShift(mirrorLine(shape.mirror(), unit), this.x), this.z.domainLength)
                : SeamTransform.translation(skew, this.z.domainLength);
        return new Step(generator, false);
    }

    private static boolean mirrors(FlatShape.@Nullable Mirror mirror, Direction.Axis axis) {
        return mirror != null && mirror.axis() == axis;
    }

    private static int mirrorLine(FlatShape.@Nullable Mirror mirror, int unit) {
        return mirror == null ? 0 : mirror.lineChunk() * unit;
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

    private static Step[] orderSteps(@Nullable Step xStep, @Nullable Step zStep, boolean mirrorsZ) {
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

    private int candidateSignX(int index) {
        return this.translations == null ? this.candidates[index].xSign() : 1;
    }

    private int candidateSignZ(int index) {
        return this.translations == null ? this.candidates[index].zSign() : 1;
    }

    private int candidateOffsetX(int index, long first, long second) {
        return this.translations == null ? this.candidates[index].xShift() : candidateShiftX(this.translations, index, first, second);
    }

    private int candidateOffsetZ(int index, long first, long second) {
        return this.translations == null ? this.candidates[index].zShift() : candidateShiftZ(this.translations, index, first, second);
    }

    private static int candidateShiftX(TranslationLattice translations, int index, long first, long second) {
        return Math.toIntExact(-translations.nearShiftX(first, second, index));
    }

    private static int candidateShiftZ(TranslationLattice translations, int index, long first, long second) {
        return Math.toIntExact(-translations.nearShiftZ(first, second, index));
    }

    private int candidateCellX(int index, long first, long second, int x) {
        return this.translations == null
                ? this.candidates[index].applyCellX(x)
                : x + candidateShiftX(this.translations, index, first, second);
    }

    private int candidateCellZ(int index, long first, long second, int z) {
        return this.translations == null
                ? this.candidates[index].applyCellZ(z)
                : z + candidateShiftZ(this.translations, index, first, second);
    }

    private double candidateX(int index, long first, long second, double x) {
        return this.translations == null
                ? this.candidates[index].applyX(x)
                : x + candidateShiftX(this.translations, index, first, second);
    }

    private double candidateZ(int index, long first, long second, double z) {
        return this.translations == null
                ? this.candidates[index].applyZ(z)
                : z + candidateShiftZ(this.translations, index, first, second);
    }

    SeamTransform foldMoveOfCell(int cellX, int cellZ) {
        return this.laps.transformOf(this.laps.ofCells(cellX, cellZ));
    }

    SeamTransform foldMoveOfCoord(double coordX, double coordZ) {
        return this.laps.transformOf(this.laps.ofCoords(coordX, coordZ));
    }

    private SeamTransform nearestTransform(long toTarget, int index, long first, long second, long toRef) {
        int signX = this.laps.signThroughX(toTarget, candidateSignX(index), toRef);
        int signZ = this.laps.signThroughZ(toTarget, candidateSignZ(index), toRef);
        int shiftX = this.laps.shiftThroughX(toTarget, candidateSignX(index), candidateOffsetX(index, first, second),
                toRef);
        int shiftZ = this.laps.shiftThroughZ(toTarget, candidateSignZ(index), candidateOffsetZ(index, first, second),
                toRef);
        return signX > 0 && signZ > 0 && shiftX == 0 && shiftZ == 0
                ? SeamTransform.IDENTITY
                : new SeamTransform(signX, signZ, shiftX, shiftZ);
    }

    SeamTransform between(int fromX, int fromZ, int toX, int toZ) {
        return foldMoveOfCell(fromX, fromZ).then(foldMoveOfCell(toX, toZ).inverse());
    }

    SeamTransform nearestMoveOfCell(int refX, int refZ, int targetX, int targetZ) {
        long toRef = this.laps.ofCells(refX, refZ);
        long toTarget = this.laps.ofCells(targetX, targetZ);
        int foldedTargetX = this.laps.foldCellX(toTarget, targetX);
        int foldedTargetZ = this.laps.foldCellZ(toTarget, targetZ);

        int deltaX = foldedTargetX - this.laps.foldCellX(toRef, refX);
        int deltaZ = foldedTargetZ - this.laps.foldCellZ(toRef, refZ);
        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        int bestIndex = nearestCellCandidate(refX, refZ, targetX, targetZ, toRef, first, second,
                foldedTargetX, foldedTargetZ);
        return bestIndex < 0
                ? SeamTransform.IDENTITY
                : nearestTransform(toTarget, bestIndex, first, second, toRef);
    }

    long nearestCellsSqrDistance(int refX, int refZ, int targetX, int targetZ) {
        long toRef = this.laps.ofCells(refX, refZ);
        long toTarget = this.laps.ofCells(targetX, targetZ);
        int foldedTargetX = this.laps.foldCellX(toTarget, targetX);
        int foldedTargetZ = this.laps.foldCellZ(toTarget, targetZ);

        int deltaX = foldedTargetX - this.laps.foldCellX(toRef, refX);
        int deltaZ = foldedTargetZ - this.laps.foldCellZ(toRef, refZ);
        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        int bestIndex = nearestCellCandidate(refX, refZ, targetX, targetZ, toRef, first, second,
                foldedTargetX, foldedTargetZ);
        if (bestIndex < 0) {
            return squared((long) targetX - refX) + squared((long) targetZ - refZ);
        }

        long copyX = this.laps.unfoldCellX(toRef, candidateCellX(bestIndex, first, second, foldedTargetX));
        long copyZ = this.laps.unfoldCellZ(toRef, candidateCellZ(bestIndex, first, second, foldedTargetZ));
        return squared(copyX - refX) + squared(copyZ - refZ);
    }

    private int nearestCellCandidate(int refX, int refZ, int targetX, int targetZ, long toRef,
            long first, long second, int foldedTargetX, int foldedTargetZ) {
        int bestIndex = -1;
        long bestDistance = squared((long) targetX - refX) + squared((long) targetZ - refZ);
        long bestShiftX = 0;
        long bestShiftZ = 0;
        for (int index = 0; index < candidateCount(); index++) {
            int inFrameX = candidateCellX(index, first, second, foldedTargetX);
            int inFrameZ = candidateCellZ(index, first, second, foldedTargetZ);
            long copyX = this.laps.unfoldCellX(toRef, inFrameX);
            long copyZ = this.laps.unfoldCellZ(toRef, inFrameZ);
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

    SeamTransform nearestMoveOfCoord(double refX, double refZ, double targetX, double targetZ) {
        long toRef = this.laps.ofCoords(refX, refZ);
        long toTarget = this.laps.ofCoords(targetX, targetZ);
        double foldedTargetX = this.laps.foldX(toTarget, targetX);
        double foldedTargetZ = this.laps.foldZ(toTarget, targetZ);

        double deltaX = foldedTargetX - this.laps.foldX(toRef, refX);
        double deltaZ = foldedTargetZ - this.laps.foldZ(toRef, refZ);
        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        int bestIndex = nearestCoordCandidate(refX, refZ, targetX, targetZ, toRef, first, second,
                foldedTargetX, foldedTargetZ);
        return bestIndex < 0
                ? SeamTransform.IDENTITY
                : nearestTransform(toTarget, bestIndex, first, second, toRef);
    }

    double nearestCoordsSqrDistance(double refX, double refZ, double targetX, double targetZ) {
        long toRef = this.laps.ofCoords(refX, refZ);
        long toTarget = this.laps.ofCoords(targetX, targetZ);
        double foldedTargetX = this.laps.foldX(toTarget, targetX);
        double foldedTargetZ = this.laps.foldZ(toTarget, targetZ);

        double deltaX = foldedTargetX - this.laps.foldX(toRef, refX);
        double deltaZ = foldedTargetZ - this.laps.foldZ(toRef, refZ);
        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        int bestIndex = nearestCoordCandidate(refX, refZ, targetX, targetZ, toRef, first, second,
                foldedTargetX, foldedTargetZ);
        if (bestIndex < 0) {
            return squared(targetX - refX) + squared(targetZ - refZ);
        }

        double copyX = this.laps.unfoldX(toRef, candidateX(bestIndex, first, second, foldedTargetX));
        double copyZ = this.laps.unfoldZ(toRef, candidateZ(bestIndex, first, second, foldedTargetZ));
        return squared(copyX - refX) + squared(copyZ - refZ);
    }

    private int nearestCoordCandidate(double refX, double refZ, double targetX, double targetZ, long toRef,
            long first, long second, double foldedTargetX, double foldedTargetZ) {
        int bestIndex = -1;
        double bestDistance = squared(targetX - refX) + squared(targetZ - refZ);
        double bestShiftX = 0.0;
        double bestShiftZ = 0.0;
        for (int index = 0; index < candidateCount(); index++) {
            double inFrameX = candidateX(index, first, second, foldedTargetX);
            double inFrameZ = candidateZ(index, first, second, foldedTargetZ);
            double copyX = this.laps.unfoldX(toRef, inFrameX);
            double copyZ = this.laps.unfoldZ(toRef, inFrameZ);
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

        return bestIndex;
    }

    double nearestBoxSqrGap(double pointX, double pointZ, AABB box) {
        double centerX = SeamBoxes.center(box.minX, box.maxX);
        double centerZ = SeamBoxes.center(box.minZ, box.maxZ);
        long toBox = this.laps.ofCoords(centerX, centerZ);
        long toPoint = this.laps.ofCoords(pointX, pointZ);

        double deltaX = this.laps.foldX(toBox, centerX) - this.laps.foldX(toPoint, pointX);
        double deltaZ = this.laps.foldZ(toBox, centerZ) - this.laps.foldZ(toPoint, pointZ);
        long first = nearFirst(deltaX, deltaZ);
        long second = nearSecond(deltaX, deltaZ);
        double best = gapSquared(box.minX, box.maxX, box.minZ, box.maxZ, pointX, pointZ);
        for (int index = 0; index < candidateCount(); index++) {
            int signX = this.laps.signThroughX(toBox, candidateSignX(index), toPoint);
            int signZ = this.laps.signThroughZ(toBox, candidateSignZ(index), toPoint);
            int shiftX = this.laps.shiftThroughX(toBox, candidateSignX(index), candidateOffsetX(index, first, second),
                    toPoint);
            int shiftZ = this.laps.shiftThroughZ(toBox, candidateSignZ(index), candidateOffsetZ(index, first, second),
                    toPoint);
            double firstX = signX * box.minX + shiftX;
            double secondX = signX * box.maxX + shiftX;
            double firstZ = signZ * box.minZ + shiftZ;
            double secondZ = signZ * box.maxZ + shiftZ;
            best = Math.min(best, gapSquared(Math.min(firstX, secondX), Math.max(firstX, secondX),
                    Math.min(firstZ, secondZ), Math.max(firstZ, secondZ), pointX, pointZ));
        }

        return best;
    }

    private static double gapSquared(double minX, double maxX, double minZ, double maxZ, double pointX,
            double pointZ) {
        double xGap = Math.max(Math.max(minX - pointX, pointX - maxX), 0.0);
        double zGap = Math.max(Math.max(minZ - pointZ, pointZ - maxZ), 0.0);
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
