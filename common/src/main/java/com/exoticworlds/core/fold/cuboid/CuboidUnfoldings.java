package com.exoticworlds.core.fold.cuboid;

import com.exoticworlds.core.fold.SeamTransform;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

final class CuboidUnfoldings {
    private static final double ON_EDGE_BLOCKS = 1.0E-7;
    private static final double ON_LINE = 1.0E-9;

    private record Edge(boolean isAcrossX, double line, double min, double max) {
        static Edge of(CuboidFold.Rect face, int side, SeamTransform toAnchor) {
            boolean isAlongZ = side == CuboidFold.WEST || side == CuboidFold.EAST;
            int fixed = switch (side) {
                case CuboidFold.WEST -> face.minX();
                case CuboidFold.EAST -> face.endX();
                case CuboidFold.NORTH -> face.minZ();
                default -> face.endZ();
            };
            double fromX = isAlongZ ? fixed : face.minX();
            double fromZ = isAlongZ ? face.minZ() : fixed;
            double toX = isAlongZ ? fixed : face.endX();
            double toZ = isAlongZ ? face.endZ() : fixed;
            double firstX = toAnchor.applyX(fromX, fromZ);
            double firstZ = toAnchor.applyZ(fromX, fromZ);
            double secondX = toAnchor.applyX(toX, toZ);
            double secondZ = toAnchor.applyZ(toX, toZ);
            return firstX == secondX
                    ? new Edge(true, firstX, Math.min(firstZ, secondZ), Math.max(firstZ, secondZ))
                    : new Edge(false, firstZ, Math.min(firstX, secondX), Math.max(firstX, secondX));
        }
    }

    private record Unfolding(SeamTransform move, List<Edge> edges) {
    }

    private static final class Search {
        private final CuboidFold.Rect[] faces;
        private final CuboidFold.Crossing[][] crossings;
        private final int target;
        private final List<Unfolding> found = new ArrayList<>();

        private Search(CuboidFold.Rect[] faces, CuboidFold.Crossing[][] crossings, int target) {
            this.faces = faces;
            this.crossings = crossings;
            this.target = target;
        }
    }

    private final CuboidFold.Rect[] faces;

    private final Unfolding[][][] moves;

    private CuboidUnfoldings(CuboidFold.Rect[] faces, Unfolding[][][] moves) {
        this.faces = faces;
        this.moves = moves;
    }

    static CuboidUnfoldings of(CuboidFold.Rect[] faces, CuboidFold.Crossing[][] crossings) {
        int count = faces.length;
        Unfolding[][][] moves = new Unfolding[count][count][];
        for (int anchor = 0; anchor < count; anchor++) {
            for (int target = 0; target < count; target++) {
                moves[anchor][target] = unfoldingsOf(faces, crossings, anchor, target);
            }
        }

        return new CuboidUnfoldings(faces, moves);
    }

    private static Unfolding[] unfoldingsOf(CuboidFold.Rect[] faces, CuboidFold.Crossing[][] crossings,
            int anchor, int target) {
        if (anchor == target) {
            return new Unfolding[] {new Unfolding(SeamTransform.IDENTITY, List.of())};
        }

        Search search = new Search(faces, crossings, target);
        walk(search, anchor, SeamTransform.IDENTITY, 1 << anchor, List.of());
        return unmovedFirst(search.found);
    }

    private static void walk(Search search, int face, SeamTransform toAnchor, int visited, List<Edge> crossed) {
        for (int side = 0; side < CuboidFold.SIDES; side++) {
            CuboidFold.Crossing crossing = search.crossings[face][side];
            int next = crossing.face();
            if ((visited & (1 << next)) != 0) {
                continue;
            }

            List<Edge> edges = new ArrayList<>(crossed);
            edges.add(Edge.of(search.faces[face], side, toAnchor));
            SeamTransform nextToAnchor = crossing.transform().inverse().then(toAnchor);
            if (next == search.target) {
                search.found.add(new Unfolding(nextToAnchor, List.copyOf(edges)));
            } else {
                walk(search, next, nextToAnchor, visited | (1 << next), edges);
            }
        }
    }

    private static Unfolding[] unmovedFirst(List<Unfolding> unfoldings) {
        for (int index = 1; index < unfoldings.size(); index++) {
            if (unfoldings.get(index).move().isIdentity()) {
                unfoldings.add(0, unfoldings.remove(index));
            }
        }

        return unfoldings.toArray(Unfolding[]::new);
    }

    SeamTransform nearest(int anchorFace, int targetFace, double anchorX, double anchorZ, double x, double z) {
        Unfolding nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Unfolding unfolding : this.moves[anchorFace][targetFace]) {
            double seatedX = unfolding.move().applyX(x, z);
            double seatedZ = unfolding.move().applyZ(x, z);
            double dx = seatedX - anchorX;
            double dz = seatedZ - anchorZ;
            double distance = dx * dx + dz * dz;
            if (distance < nearestDistance && isCrossingEveryEdge(unfolding.edges(), anchorX, anchorZ, dx, dz)) {
                nearest = unfolding;
                nearestDistance = distance;
            }
        }

        return nearest == null ? SeamTransform.IDENTITY : nearest.move();
    }

    @Nullable SeamTransform moveAlongLine(int anchorFace, double anchorX, double anchorZ, double x, double z,
            int blockX, int blockZ) {
        for (int targetFace = 0; targetFace < this.faces.length; targetFace++) {
            if (targetFace == anchorFace) {
                continue;
            }

            SeamTransform back = lineUnfoldingBack(anchorFace, targetFace, anchorX, anchorZ, x, z, blockX, blockZ);
            if (back != null) {
                return back;
            }
        }

        return null;
    }

    private @Nullable SeamTransform lineUnfoldingBack(int anchorFace, int targetFace, double anchorX, double anchorZ,
            double x, double z, int blockX, int blockZ) {
        for (Unfolding unfolding : this.moves[anchorFace][targetFace]) {
            SeamTransform back = unfolding.move().inverse();
            boolean isSeatedOnTarget = this.faces[targetFace].contains(
                    back.applyCellX(blockX, blockZ), back.applyCellZ(blockX, blockZ));
            if (isSeatedOnTarget
                    && isCrossingEveryEdge(unfolding.edges(), anchorX, anchorZ, x - anchorX, z - anchorZ)) {
                return back;
            }
        }

        return null;
    }

    private static boolean isCrossingEveryEdge(List<Edge> edges, double anchorX, double anchorZ, double dx,
            double dz) {
        double last = 0.0;
        for (int index = 0; index < edges.size(); index++) {
            double at = crossingAt(edges.get(index), anchorX, anchorZ, dx, dz, last);
            if (Double.isNaN(at)) {
                return false;
            }

            last = at;
        }

        return true;
    }

    private static double crossingAt(Edge edge, double anchorX, double anchorZ, double dx, double dz, double after) {
        double start = edge.isAcrossX() ? anchorX : anchorZ;
        double toward = edge.isAcrossX() ? dx : dz;
        if (toward == 0.0) {
            return Math.abs(start - edge.line()) <= ON_EDGE_BLOCKS ? after : Double.NaN;
        }

        double at = (edge.line() - start) / toward;
        double along = edge.isAcrossX() ? anchorZ + at * dz : anchorX + at * dx;
        boolean isOnSegment = at >= after - ON_LINE && at <= 1.0 + ON_LINE
                && along >= edge.min() - ON_EDGE_BLOCKS && along <= edge.max() + ON_EDGE_BLOCKS;
        return isOnSegment ? at : Double.NaN;
    }
}
