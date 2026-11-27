package com.exoticworlds.core.fold.cuboid;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.SeamBoxes;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.fold.cuboid.CuboidFold.Crossing;
import com.exoticworlds.core.fold.cuboid.CuboidFold.Rect;
import com.exoticworlds.core.fold.WorldFold.Folded;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

final class CuboidRegions {
    private static final int LAP_CROSSINGS = 4;

    private record Piece(double minX, double minZ, double maxX, double maxZ, SeamTransform applied, int crossings) {
        static Piece of(AABB box) {
            return new Piece(box.minX, box.minZ, box.maxX, box.maxZ, SeamTransform.IDENTITY, 0);
        }

        static Piece of(BoundingBox region) {
            return new Piece(region.minX(), region.minZ(), region.maxX() + 1, region.maxZ() + 1,
                    SeamTransform.IDENTITY, 0);
        }

        Piece carried(SeamTransform transform) {
            double firstX = transform.applyX(this.minX, this.minZ);
            double secondX = transform.applyX(this.maxX, this.maxZ);
            double firstZ = transform.applyZ(this.minX, this.minZ);
            double secondZ = transform.applyZ(this.maxX, this.maxZ);
            return new Piece(Math.min(firstX, secondX), Math.min(firstZ, secondZ),
                    Math.max(firstX, secondX), Math.max(firstZ, secondZ),
                    this.applied.then(transform), this.crossings + 1);
        }

        @Nullable Piece between(double fromX, double toX, double fromZ, double toZ) {
            double[] x = overlap(this.minX, this.maxX, fromX, toX);
            double[] z = overlap(this.minZ, this.maxZ, fromZ, toZ);
            return x == null || z == null
                    ? null
                    : new Piece(x[0], z[0], x[1], z[1], this.applied, this.crossings);
        }

        boolean meets(Piece other) {
            return this.minX < other.maxX && other.minX < this.maxX
                    && this.minZ < other.maxZ && other.minZ < this.maxZ;
        }

        AABB box(AABB source) {
            return new AABB(this.minX, source.minY, this.minZ, this.maxX, source.maxY, this.maxZ);
        }

        BoundingBox region(BoundingBox source) {
            return new BoundingBox((int) this.minX, source.minY(), (int) this.minZ,
                    (int) this.maxX - 1, source.maxY(), (int) this.maxZ - 1);
        }
    }

    private static final class Cut {
        private final List<Piece> pieces = new ArrayList<>();

        private boolean wraps;
    }

    private final Rect[] faces;

    private final Crossing[][] crossings;

    private final double[] xLines;

    private final double[] zLines;

    CuboidRegions(Rect[] faces, Crossing[][] crossings) {
        this.faces = faces;
        this.crossings = crossings;
        Set<Integer> x = new TreeSet<>();
        Set<Integer> z = new TreeSet<>();
        for (Rect face : faces) {
            x.add(face.minX());
            x.add(face.endX());
            z.add(face.minZ());
            z.add(face.endZ());
        }

        this.xLines = linesOf(x);
        this.zLines = linesOf(z);
    }

    private static double[] linesOf(Set<Integer> bounds) {
        double[] lines = new double[bounds.size() + 2];
        int index = 0;
        lines[index++] = Double.NEGATIVE_INFINITY;
        for (int bound : bounds) {
            lines[index++] = bound;
        }

        lines[index] = Double.POSITIVE_INFINITY;
        return lines;
    }

    private static double @Nullable [] overlap(double low, double high, double from, double to) {
        double start = Math.max(low, from);
        double end = Math.min(high, to);
        boolean pointInside = low == high && from <= low && low < to;
        return start < end || pointInside ? new double[] {start, end} : null;
    }

    boolean isCrossing(AABB box) {
        return !seatedOnOneFace(Piece.of(box));
    }

    boolean isCrossing(BoundingBox region) {
        return !seatedOnOneFace(Piece.of(region));
    }

    private boolean seatedOnOneFace(Piece whole) {
        for (Rect face : this.faces) {
            if (face.minX() <= whole.minX() && whole.maxX() <= face.endX()
                    && face.minZ() <= whole.minZ() && whole.maxZ() <= face.endZ()) {
                return true;
            }
        }

        return false;
    }

    List<Folded<AABB>> split(AABB box) {
        if (!isCrossing(box)) {
            return List.of(Folded.of(box));
        }

        List<Piece> pieces = cut(Piece.of(box)).pieces;
        List<Folded<AABB>> folded = new ArrayList<>(pieces.size());
        for (Piece piece : pieces) {
            folded.add(new Folded<>(piece.box(box), piece.applied().getOrientation()));
        }

        return folded;
    }

    List<Folded<BoundingBox>> split(BoundingBox region) {
        if (!isCrossing(region)) {
            return List.of(Folded.of(region));
        }

        List<Piece> pieces = cut(Piece.of(region)).pieces;
        List<Folded<BoundingBox>> folded = new ArrayList<>(pieces.size());
        for (Piece piece : pieces) {
            folded.add(new Folded<>(piece.region(region), piece.applied().getOrientation()));
        }

        return folded;
    }

    boolean areRegionsOverlapping(BoundingBox first, BoundingBox second) {
        if (first.minY() > second.maxY() || second.minY() > first.maxY()) {
            return false;
        }

        List<Piece> secondPieces = cut(Piece.of(second)).pieces;
        for (Piece piece : cut(Piece.of(first)).pieces) {
            if (meetsAny(piece, secondPieces, 0)) {
                return true;
            }
        }

        return false;
    }

    List<DeckTransformation> copiesTouching(BoundingBox region, int reach) {
        if (reach < 0) {
            throw new IllegalArgumentException("A copy reach is never negative, got " + reach);
        }

        Set<SeamTransform> moves = new LinkedHashSet<>();
        for (Piece piece : cut(Piece.of(region)).pieces) {
            if (piece.crossings() <= reach) {
                moves.add(piece.applied().inverse());
            }
        }

        List<DeckTransformation> copies = new ArrayList<>(moves.size());
        for (SeamTransform move : moves) {
            if (move.isIdentity()) {
                copies.addFirst(DeckTransformation.IDENTITY);
            } else {
                copies.add(new DeckTransformation(move));
            }
        }

        return copies;
    }

    boolean isFoldedOntoItself(BoundingBox region) {
        Cut cut = cut(Piece.of(region));
        if (cut.wraps) {
            return true;
        }

        for (int index = 0; index < cut.pieces.size(); index++) {
            if (meetsAny(cut.pieces.get(index), cut.pieces, index + 1)) {
                return true;
            }
        }

        return false;
    }

    private static boolean meetsAny(Piece piece, List<Piece> others, int from) {
        for (int index = from; index < others.size(); index++) {
            if (piece.meets(others.get(index))) {
                return true;
            }
        }

        return false;
    }

    private Cut cut(Piece whole) {
        Cut cut = new Cut();
        for (int column = 0; column + 1 < this.xLines.length; column++) {
            for (int row = 0; row + 1 < this.zLines.length; row++) {
                Piece cell = whole.between(
                        this.xLines[column], this.xLines[column + 1], this.zLines[row], this.zLines[row + 1]);
                if (cell != null) {
                    seat(cut, cell);
                }
            }
        }

        return cut;
    }

    private void seat(Cut cut, Piece cell) {
        double x = SeamBoxes.center(cell.minX(), cell.maxX());
        double z = SeamBoxes.center(cell.minZ(), cell.maxZ());
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        if (CuboidFold.faceOf(this.faces, blockX, blockZ) != CuboidFold.NO_FACE) {
            cut.pieces.add(cell);
            return;
        }

        int[] bands = new int[this.faces.length * CuboidFold.SIDES];
        int count = 0;
        for (int band = 0; band < bands.length; band++) {
            Rect face = this.faces[band / CuboidFold.SIDES];
            if (face.bandDistance(band % CuboidFold.SIDES, x, z, blockX, blockZ) != CuboidFold.NOT_PAST) {
                bands[count++] = band;
            }
        }

        boolean carried = false;
        for (int index = 0; index < count; index++) {
            if (!hasNearerBand(cell, bands, count, index)) {
                walk(cut, cell, bands[index]);
                carried = true;
            }
        }

        if (!carried) {
            cut.pieces.add(cell);
        }
    }

    private boolean hasNearerBand(Piece cell, int[] bands, int count, int asked) {
        double nearest = nearest(cell, bands[asked]);
        for (int index = 0; index < count; index++) {
            if (index != asked && farthest(cell, bands[index]) <= nearest) {
                return true;
            }
        }

        return false;
    }

    private double nearest(Piece cell, int band) {
        Rect face = this.faces[band / CuboidFold.SIDES];
        return switch (band % CuboidFold.SIDES) {
            case CuboidFold.WEST -> face.minX() - cell.maxX();
            case CuboidFold.EAST -> cell.minX() - face.endX();
            case CuboidFold.NORTH -> face.minZ() - cell.maxZ();
            default -> cell.minZ() - face.endZ();
        };
    }

    private double farthest(Piece cell, int band) {
        Rect face = this.faces[band / CuboidFold.SIDES];
        return switch (band % CuboidFold.SIDES) {
            case CuboidFold.WEST -> face.minX() - cell.minX();
            case CuboidFold.EAST -> cell.maxX() - face.endX();
            case CuboidFold.NORTH -> face.minZ() - cell.minZ();
            default -> cell.maxZ() - face.endZ();
        };
    }

    private void walk(Cut cut, Piece cell, int band) {
        int face = band / CuboidFold.SIDES;
        int side = band % CuboidFold.SIDES;
        Piece ahead = cell;
        for (int crossed = 0; crossed < LAP_CROSSINGS; crossed++) {
            Crossing crossing = this.crossings[face][side];
            Piece carried = ahead.carried(crossing.transform());
            face = crossing.face();
            side = CuboidFold.opposite(crossing.entrySide());
            Rect entered = this.faces[face];
            Piece seated = carried.between(entered.minX(), entered.endX(), entered.minZ(), entered.endZ());
            if (seated != null) {
                cut.pieces.add(seated);
            }

            ahead = past(carried, entered, side);
            if (ahead == null) {
                return;
            }
        }

        cut.wraps = true;
    }

    private static @Nullable Piece past(Piece piece, Rect face, int side) {
        double everywhere = Double.POSITIVE_INFINITY;
        return switch (side) {
            case CuboidFold.WEST -> piece.between(-everywhere, face.minX(), -everywhere, everywhere);
            case CuboidFold.EAST -> piece.between(face.endX(), everywhere, -everywhere, everywhere);
            case CuboidFold.NORTH -> piece.between(-everywhere, everywhere, -everywhere, face.minZ());
            default -> piece.between(-everywhere, everywhere, face.endZ(), everywhere);
        };
    }
}
