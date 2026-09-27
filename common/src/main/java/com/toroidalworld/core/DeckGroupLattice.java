package com.toroidalworld.core;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

final class DeckGroupLattice {
    private static final int CANDIDATE_REACH = 2;

    private static final int COORD_FOLD_ATTEMPTS = 3;

    record CoordPiece(double minX, double maxX, double minZ, double maxZ, SeamTransform applied) {}

    record CellPiece(int minX, int maxX, int minZ, int maxZ, SeamTransform applied) {}

    private record Step(SeamTransform generator, boolean onX) {}

    final WrapDomain x;

    final WrapDomain z;

    private final Step[] steps;

    private final SeamTransform[] candidates;

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

        SeamTransform best = SeamTransform.IDENTITY;
        long bestDistance = squared((long) targetX - refX) + squared((long) targetZ - refZ);
        long bestShiftX = 0;
        long bestShiftZ = 0;
        for (SeamTransform candidate : this.candidates) {
            int inFrameX = candidate.applyCellX(foldedTargetX);
            int inFrameZ = candidate.applyCellZ(foldedTargetZ);
            long copyX = fromRef.applyCellX(inFrameX);
            long copyZ = fromRef.applyCellZ(inFrameZ);
            long distance = squared(copyX - refX) + squared(copyZ - refZ);
            long shiftX = copyX - targetX;
            long shiftZ = copyZ - targetZ;
            if (distance < bestDistance || (distance == bestDistance
                    && closerToRest(shiftX, shiftZ, bestShiftX, bestShiftZ))) {
                best = toTarget.then(candidate).then(fromRef);
                bestDistance = distance;
                bestShiftX = shiftX;
                bestShiftZ = shiftZ;
            }
        }

        return best;
    }

    SeamTransform nearestCoords(double refX, double refZ, double targetX, double targetZ) {
        SeamTransform toRef = foldCoords(refX, refZ);
        SeamTransform toTarget = foldCoords(targetX, targetZ);
        SeamTransform fromRef = toRef.inverse();
        double foldedTargetX = toTarget.applyX(targetX);
        double foldedTargetZ = toTarget.applyZ(targetZ);

        SeamTransform best = SeamTransform.IDENTITY;
        double bestDistance = squared(targetX - refX) + squared(targetZ - refZ);
        double bestShiftX = 0.0;
        double bestShiftZ = 0.0;
        for (SeamTransform candidate : this.candidates) {
            double inFrameX = candidate.applyX(foldedTargetX);
            double inFrameZ = candidate.applyZ(foldedTargetZ);
            double copyX = fromRef.applyX(inFrameX);
            double copyZ = fromRef.applyZ(inFrameZ);
            double distance = squared(copyX - refX) + squared(copyZ - refZ);
            double shiftX = copyX - targetX;
            double shiftZ = copyZ - targetZ;
            if (distance < bestDistance || (distance == bestDistance
                    && closerToRest(shiftX, shiftZ, bestShiftX, bestShiftZ))) {
                best = toTarget.then(candidate).then(fromRef);
                bestDistance = distance;
                bestShiftX = shiftX;
                bestShiftZ = shiftZ;
            }
        }

        return best;
    }

    double nearestBoxGap(double pointX, double pointZ, double minX, double maxX, double minZ, double maxZ) {
        SeamTransform toBox = foldCoords((minX + maxX) / 2.0, (minZ + maxZ) / 2.0);
        SeamTransform fromPoint = foldCoords(pointX, pointZ).inverse();

        double best = gapSquared(minX, maxX, minZ, maxZ, pointX, pointZ);
        for (SeamTransform candidate : this.candidates) {
            SeamTransform move = toBox.then(candidate).then(fromPoint);
            double firstX = move.applyX(minX);
            double secondX = move.applyX(maxX);
            double firstZ = move.applyZ(minZ);
            double secondZ = move.applyZ(maxZ);
            best = Math.min(best, gapSquared(
                    Math.min(firstX, secondX), Math.max(firstX, secondX),
                    Math.min(firstZ, secondZ), Math.max(firstZ, secondZ),
                    pointX, pointZ));
        }

        return best;
    }

    private static double gapSquared(double minX, double maxX, double minZ, double maxZ,
            double pointX, double pointZ) {
        double xGap = Math.max(Math.max(minX - pointX, pointX - maxX), 0.0);
        double zGap = Math.max(Math.max(minZ - pointZ, pointZ - maxZ), 0.0);
        return xGap * xGap + zGap * zGap;
    }

    List<CoordPiece> splitCoords(double minX, double maxX, double minZ, double maxZ) {
        List<CoordPiece> pieces = new ArrayList<>();
        pieces.add(new CoordPiece(minX, maxX, minZ, maxZ, SeamTransform.IDENTITY));
        for (Step step : this.steps) {
            WrapDomain domain = step.onX() ? this.x : this.z;
            List<CoordPiece> next = new ArrayList<>();
            for (CoordPiece piece : pieces) {
                double low = step.onX() ? piece.minX() : piece.minZ();
                double high = step.onX() ? piece.maxX() : piece.maxZ();
                for (double[] slice : sliceSpan(domain, low, high)) {
                    long laps = (long) Math.floor((slice[0] - domain.lowerBound) / domain.domainLength);
                    SeamTransform move = step.generator().power(Math.toIntExact(-laps));
                    double lowX = step.onX() ? slice[0] : piece.minX();
                    double highX = step.onX() ? slice[1] : piece.maxX();
                    double lowZ = step.onX() ? piece.minZ() : slice[0];
                    double highZ = step.onX() ? piece.maxZ() : slice[1];
                    double firstX = move.applyX(lowX);
                    double secondX = move.applyX(highX);
                    double firstZ = move.applyZ(lowZ);
                    double secondZ = move.applyZ(highZ);
                    next.add(new CoordPiece(
                            Math.min(firstX, secondX), Math.max(firstX, secondX),
                            Math.min(firstZ, secondZ), Math.max(firstZ, secondZ),
                            piece.applied().then(move)));
                }
            }

            pieces = next;
        }

        return pieces;
    }

    List<CellPiece> splitCells(int minX, int maxX, int minZ, int maxZ) {
        List<CellPiece> pieces = new ArrayList<>();
        pieces.add(new CellPiece(minX, maxX, minZ, maxZ, SeamTransform.IDENTITY));
        for (Step step : this.steps) {
            WrapDomain domain = step.onX() ? this.x : this.z;
            List<CellPiece> next = new ArrayList<>();
            for (CellPiece piece : pieces) {
                int low = step.onX() ? piece.minX() : piece.minZ();
                int high = step.onX() ? piece.maxX() : piece.maxZ();
                for (int[] slice : sliceCellSpan(domain, low, high)) {
                    long laps = Math.floorDiv((long) slice[0] - domain.lowerBound, domain.domainLength);
                    SeamTransform move = step.generator().power(Math.toIntExact(-laps));
                    int lowX = step.onX() ? slice[0] : piece.minX();
                    int highX = step.onX() ? slice[1] : piece.maxX();
                    int lowZ = step.onX() ? piece.minZ() : slice[0];
                    int highZ = step.onX() ? piece.maxZ() : slice[1];
                    int firstX = move.applyCellX(lowX);
                    int secondX = move.applyCellX(highX);
                    int firstZ = move.applyCellZ(lowZ);
                    int secondZ = move.applyCellZ(highZ);
                    next.add(new CellPiece(
                            Math.min(firstX, secondX), Math.max(firstX, secondX),
                            Math.min(firstZ, secondZ), Math.max(firstZ, secondZ),
                            piece.applied().then(move)));
                }
            }

            pieces = next;
        }

        return pieces;
    }

    List<DeckTransformation> copiesTouching(int minX, int maxX, int minZ, int maxZ, int reach) {
        if (this.steps.length == 0) {
            return List.of(DeckTransformation.IDENTITY);
        }

        List<DeckTransformation> copies = new ArrayList<>();
        Step first = this.steps[0];
        Step second = this.steps.length > 1 ? this.steps[1] : null;
        for (int firstPower = -reach; firstPower <= reach; firstPower++) {
            SeamTransform moved = first.generator().power(firstPower);
            int[] worldX = worldImage(this.x,
                    moved.applyCellX(this.x.lowerBound), moved.applyCellX(this.x.upperBound - 1));
            int[] worldZ = worldImage(this.z,
                    moved.applyCellZ(this.z.lowerBound), moved.applyCellZ(this.z.upperBound - 1));
            if (second == null) {
                if (meets(worldX, minX, maxX) && meets(worldZ, minZ, maxZ)) {
                    addCopy(copies, moved);
                }

                continue;
            }

            boolean alongX = second.onX();
            if (!meets(alongX ? worldZ : worldX, alongX ? minZ : minX, alongX ? maxZ : maxX)) {
                continue;
            }

            int[] worldAlong = alongX ? worldX : worldZ;
            int[] laps = (alongX ? this.x : this.z).lapsBetween(
                    worldAlong[0], worldAlong[1], alongX ? minX : minZ, alongX ? maxX : maxZ);
            int lowestLap = Math.max(laps[0], -reach);
            int highestLap = Math.min(laps[1], reach);
            for (int secondPower = lowestLap; secondPower <= highestLap; secondPower++) {
                addCopy(copies, moved.then(second.generator().power(secondPower)));
            }
        }

        return copies;
    }

    boolean foldsOntoItself(int minX, int maxX, int minZ, int maxZ) {
        if (this.steps.length == 0) {
            return false;
        }

        Step first = this.steps[0];
        Step second = this.steps.length > 1 ? this.steps[1] : null;
        WrapDomain own = first.onX() ? this.x : this.z;
        long extent = first.onX() ? (long) maxX - minX : (long) maxZ - minZ;
        int highestPower = Math.toIntExact(extent / own.domainLength);
        for (int firstPower = -highestPower; firstPower <= highestPower; firstPower++) {
            SeamTransform moved = first.generator().power(firstPower);
            CellPiece image = movedCells(moved, minX, maxX, minZ, maxZ);
            if (second == null) {
                if (firstPower != 0 && meets(image.minX(), image.maxX(), minX, maxX)
                        && meets(image.minZ(), image.maxZ(), minZ, maxZ)) {
                    return true;
                }

                continue;
            }

            int[] laps = second.onX()
                    ? this.x.lapsBetween(image.minX(), image.maxX(), minX, maxX)
                    : this.z.lapsBetween(image.minZ(), image.maxZ(), minZ, maxZ);
            if (laps[0] <= laps[1] && (firstPower != 0 || laps[0] < 0 || laps[1] > 0)) {
                return true;
            }
        }

        return false;
    }

    private static int[] worldImage(WrapDomain domain, int firstCell, int secondCell) {
        return domain instanceof WrapDomain.Noop
                ? null
                : new int[] {Math.min(firstCell, secondCell), Math.max(firstCell, secondCell)};
    }

    private static boolean meets(int[] span, int min, int max) {
        return span == null || meets(span[0], span[1], min, max);
    }

    static boolean meets(int firstMin, int firstMax, int secondMin, int secondMax) {
        return firstMin <= secondMax && secondMin <= firstMax;
    }

    private static void addCopy(List<DeckTransformation> copies, SeamTransform element) {
        if (element.isIdentity()) {
            copies.addFirst(DeckTransformation.IDENTITY);
        } else {
            copies.add(new DeckTransformation(element));
        }
    }

    private static CellPiece movedCells(SeamTransform move, int minX, int maxX, int minZ, int maxZ) {
        int firstX = move.applyCellX(minX);
        int secondX = move.applyCellX(maxX);
        int firstZ = move.applyCellZ(minZ);
        int secondZ = move.applyCellZ(maxZ);
        return new CellPiece(
                Math.min(firstX, secondX), Math.max(firstX, secondX),
                Math.min(firstZ, secondZ), Math.max(firstZ, secondZ), move);
    }

    private static List<double[]> sliceSpan(WrapDomain domain, double min, double max) {
        double length = Math.min(max - min, domain.domainLength);
        double end = min + length;
        List<double[]> slices = new ArrayList<>();
        double cursor = min;
        while (cursor < end) {
            long laps = (long) Math.floor((cursor - domain.lowerBound) / domain.domainLength);
            double lapEnd = domain.lowerBound + (laps + 1) * (double) domain.domainLength;
            double sliceEnd = Math.min(end, lapEnd);
            slices.add(new double[] {cursor, sliceEnd});
            cursor = sliceEnd;
        }

        return slices;
    }

    private static List<int[]> sliceCellSpan(WrapDomain domain, int min, int max) {
        long length = Math.min((long) max - min + 1, domain.domainLength);
        long end = min + length - 1;
        List<int[]> slices = new ArrayList<>();
        long cursor = min;
        while (cursor <= end) {
            long laps = Math.floorDiv(cursor - domain.lowerBound, domain.domainLength);
            long lapEnd = domain.lowerBound + (laps + 1) * (long) domain.domainLength - 1;
            long sliceEnd = Math.min(end, lapEnd);
            slices.add(new int[] {(int) cursor, (int) sliceEnd});
            cursor = sliceEnd + 1;
        }

        return slices;
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
