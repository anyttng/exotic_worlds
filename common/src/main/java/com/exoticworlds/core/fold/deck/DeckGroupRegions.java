package com.exoticworlds.core.fold.deck;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.core.fold.deck.DeckGroupLattice.Step;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

final class DeckGroupRegions {
    record CoordPiece(AABB box, SeamTransform applied) {}

    record CellPiece(BoundingBox box, SeamTransform applied) {}

    private final WrapDomain x;

    private final WrapDomain z;

    private final Step[] steps;

    DeckGroupRegions(WrapDomain x, WrapDomain z, Step[] steps) {
        this.x = x;
        this.z = z;
        this.steps = steps;
    }

    List<CoordPiece> splitCoords(AABB box) {
        List<CoordPiece> pieces = new ArrayList<>();
        pieces.add(new CoordPiece(box, SeamTransform.IDENTITY));
        for (Step step : this.steps) {
            WrapDomain domain = step.isOnX() ? this.x : this.z;
            List<CoordPiece> next = new ArrayList<>();
            for (CoordPiece piece : pieces) {
                AABB span = piece.box();
                double low = step.isOnX() ? span.minX : span.minZ;
                double high = step.isOnX() ? span.maxX : span.maxZ;
                for (double[] slice : sliceSpan(domain, low, high)) {
                    long laps = (long) Math.floor((slice[0] - domain.lowerBound) / domain.domainLength);
                    SeamTransform move = step.generator().power(Math.toIntExact(-laps));
                    AABB sliced = step.isOnX()
                            ? new AABB(slice[0], span.minY, span.minZ, slice[1], span.maxY, span.maxZ)
                            : new AABB(span.minX, span.minY, slice[0], span.maxX, span.maxY, slice[1]);
                    next.add(new CoordPiece(move.apply(sliced), piece.applied().then(move)));
                }
            }

            pieces = next;
        }

        return pieces;
    }

    List<CellPiece> splitCells(BoundingBox region) {
        List<CellPiece> pieces = new ArrayList<>();
        pieces.add(new CellPiece(region, SeamTransform.IDENTITY));
        for (Step step : this.steps) {
            WrapDomain domain = step.isOnX() ? this.x : this.z;
            List<CellPiece> next = new ArrayList<>();
            for (CellPiece piece : pieces) {
                BoundingBox span = piece.box();
                int low = step.isOnX() ? span.minX() : span.minZ();
                int high = step.isOnX() ? span.maxX() : span.maxZ();
                for (int[] slice : sliceCellSpan(domain, low, high)) {
                    long laps = Math.floorDiv((long) slice[0] - domain.lowerBound, domain.domainLength);
                    SeamTransform move = step.generator().power(Math.toIntExact(-laps));
                    BoundingBox sliced = step.isOnX()
                            ? new BoundingBox(slice[0], span.minY(), span.minZ(), slice[1], span.maxY(), span.maxZ())
                            : new BoundingBox(span.minX(), span.minY(), slice[0], span.maxX(), span.maxY(), slice[1]);
                    next.add(new CellPiece(move.apply(sliced), piece.applied().then(move)));
                }
            }

            pieces = next;
        }

        return pieces;
    }

    List<DeckTransformation> copiesTouching(int minX, int maxX, int minZ, int maxZ, int reachLaps) {
        if (this.steps.length == 0) {
            return List.of(DeckTransformation.IDENTITY);
        }

        List<DeckTransformation> copies = new ArrayList<>();
        Step first = this.steps[0];
        Step second = this.steps.length > 1 ? this.steps[1] : null;
        int[] firstLaps = first.isOnX() ? this.x.laps(minX, maxX) : this.z.laps(minZ, maxZ);
        int lowestFirst = Math.max(firstLaps[0], -reachLaps);
        int highestFirst = Math.min(firstLaps[1], reachLaps);
        for (int firstPower = lowestFirst; firstPower <= highestFirst; firstPower++) {
            SeamTransform moved = first.generator().power(firstPower);
            int lowX = this.x.lowerBound;
            int lowZ = this.z.lowerBound;
            int highX = this.x.upperBound - 1;
            int highZ = this.z.upperBound - 1;
            int[] worldX = worldImage(this.x, moved.applyCellX(lowX, lowZ), moved.applyCellX(highX, highZ));
            int[] worldZ = worldImage(this.z, moved.applyCellZ(lowX, lowZ), moved.applyCellZ(highX, highZ));
            if (second == null) {
                if (isMeeting(worldX, minX, maxX) && isMeeting(worldZ, minZ, maxZ)) {
                    addCopy(copies, moved);
                }

                continue;
            }

            boolean isAlongX = second.isOnX();
            if (!isMeeting(isAlongX ? worldZ : worldX, isAlongX ? minZ : minX, isAlongX ? maxZ : maxX)) {
                continue;
            }

            int[] worldAlong = Objects.requireNonNull(isAlongX ? worldX : worldZ, "a step's axis loops");
            int[] laps = (isAlongX ? this.x : this.z).lapsBetween(
                    worldAlong[0], worldAlong[1], isAlongX ? minX : minZ, isAlongX ? maxX : maxZ);
            int lowestLap = Math.max(laps[0], -reachLaps);
            int highestLap = Math.min(laps[1], reachLaps);
            for (int secondPower = lowestLap; secondPower <= highestLap; secondPower++) {
                addCopy(copies, moved.then(second.generator().power(secondPower)));
            }
        }

        return copies;
    }

    boolean isFoldedOntoItself(BoundingBox region) {
        if (this.steps.length == 0) {
            return false;
        }

        int minX = region.minX();
        int maxX = region.maxX();
        int minZ = region.minZ();
        int maxZ = region.maxZ();
        Step first = this.steps[0];
        Step second = this.steps.length > 1 ? this.steps[1] : null;
        WrapDomain own = first.isOnX() ? this.x : this.z;
        long extent = first.isOnX() ? (long) maxX - minX : (long) maxZ - minZ;
        int highestPower = Math.toIntExact(extent / own.domainLength);
        for (int firstPower = -highestPower; firstPower <= highestPower; firstPower++) {
            BoundingBox image = first.generator().power(firstPower).apply(region);
            if (second == null) {
                if (firstPower != 0 && isMeeting(image.minX(), image.maxX(), minX, maxX)
                        && isMeeting(image.minZ(), image.maxZ(), minZ, maxZ)) {
                    return true;
                }

                continue;
            }

            int[] laps = second.isOnX()
                    ? this.x.lapsBetween(image.minX(), image.maxX(), minX, maxX)
                    : this.z.lapsBetween(image.minZ(), image.maxZ(), minZ, maxZ);
            if (laps[0] <= laps[1] && (firstPower != 0 || laps[0] < 0 || laps[1] > 0)) {
                return true;
            }
        }

        return false;
    }

    private static int @Nullable [] worldImage(WrapDomain domain, int firstCell, int secondCell) {
        return domain instanceof WrapDomain.Noop
                ? null
                : new int[] {Math.min(firstCell, secondCell), Math.max(firstCell, secondCell)};
    }

    private static boolean isMeeting(int @Nullable [] span, int min, int max) {
        return span == null || isMeeting(span[0], span[1], min, max);
    }

    static boolean isMeeting(int firstMin, int firstMax, int secondMin, int secondMax) {
        return firstMin <= secondMax && secondMin <= firstMax;
    }

    private static void addCopy(List<DeckTransformation> copies, SeamTransform element) {
        if (element.isIdentity()) {
            copies.addFirst(DeckTransformation.IDENTITY);
        } else {
            copies.add(new DeckTransformation(element));
        }
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
}
