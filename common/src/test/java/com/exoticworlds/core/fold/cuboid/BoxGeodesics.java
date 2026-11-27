package com.exoticworlds.core.fold.cuboid;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.core.fold.cuboid.CuboidFace.BoxVector;

final class BoxGeodesics {
    private static final int AXES = 3;
    private static final double ON_EDGE = 1.0E-9;

    private record BoxSide(int axis, double value) {
        boolean isNear() {
            return this.value == 0.0;
        }
    }

    private final double[] size;

    private BoxGeodesics(double[] size) {
        this.size = size;
    }

    static BoxGeodesics of(CuboidAtlas atlas) {
        CuboidFace front = CuboidFixtures.face(atlas, CuboidFace.Side.FRONT);
        CuboidFace west = CuboidFixtures.face(atlas, CuboidFace.Side.WEST);
        return new BoxGeodesics(new double[] {front.widthBlocks(), west.depthBlocks(), west.widthBlocks()});
    }

    double shortestBlocks(CuboidFace from, double fromX, double fromZ, CuboidFace to, double toX, double toZ) {
        BoxSide start = sideOf(from);
        BoxSide end = sideOf(to);
        double[] source = pointOf(from, fromX, fromZ);
        double[] target = pointOf(to, toX, toZ);
        double shortest = Double.MAX_VALUE;
        for (List<BoxSide> path : pathsBetween(start, end)) {
            shortest = Math.min(shortest, straightBlocks(path, source, target));
        }

        return shortest;
    }

    private static BoxSide sideOf(CuboidFace face) {
        for (int axis = 0; axis < AXES; axis++) {
            if (component(face.alongX(), axis) == 0 && component(face.alongZ(), axis) == 0) {
                return new BoxSide(axis, component(face.origin(), axis));
            }
        }

        throw new IllegalArgumentException(face.side() + " spans every axis");
    }

    private static double[] pointOf(CuboidFace face, double x, double z) {
        double localX = x - face.minBlockX();
        double localZ = z - face.minBlockZ();
        double[] point = new double[AXES];
        for (int axis = 0; axis < AXES; axis++) {
            point[axis] = component(face.origin(), axis) + localX * component(face.alongX(), axis)
                    + localZ * component(face.alongZ(), axis);
        }

        return point;
    }

    private static int component(BoxVector vector, int axis) {
        return switch (axis) {
            case 0 -> vector.a();
            case 1 -> vector.b();
            default -> vector.c();
        };
    }

    private List<List<BoxSide>> pathsBetween(BoxSide start, BoxSide end) {
        List<List<BoxSide>> paths = new ArrayList<>();
        List<BoxSide> path = new ArrayList<>();
        path.add(start);
        extend(path, end, paths);
        return paths;
    }

    private void extend(List<BoxSide> path, BoxSide end, List<List<BoxSide>> paths) {
        BoxSide last = path.get(path.size() - 1);
        if (last.equals(end)) {
            paths.add(List.copyOf(path));
            return;
        }

        for (BoxSide next : sides()) {
            if (next.axis() != last.axis() && !path.contains(next)) {
                path.add(next);
                extend(path, end, paths);
                path.remove(path.size() - 1);
            }
        }
    }

    private List<BoxSide> sides() {
        List<BoxSide> sides = new ArrayList<>();
        for (int axis = 0; axis < AXES; axis++) {
            sides.add(new BoxSide(axis, 0.0));
            sides.add(new BoxSide(axis, this.size[axis]));
        }

        return sides;
    }

    private double straightBlocks(List<BoxSide> path, double[] source, double[] target) {
        BoxSide first = path.get(0);
        double[] from = flat(first, source);
        double[] to = flat(first, intoFirst(path, path.size() - 1, target));
        double last = -ON_EDGE;
        for (int index = 0; index + 1 < path.size(); index++) {
            double[][] edge = edgeBetween(path.get(index), path.get(index + 1));
            double[] edgeFrom = flat(first, intoFirst(path, index, edge[0]));
            double[] edgeTo = flat(first, intoFirst(path, index, edge[1]));
            double at = crossingAt(from, to, edgeFrom, edgeTo);
            if (Double.isNaN(at) || at < last - ON_EDGE) {
                return Double.MAX_VALUE;
            }

            last = at;
        }

        return Math.hypot(to[0] - from[0], to[1] - from[1]);
    }

    private static double[] intoFirst(List<BoxSide> path, int index, double[] point) {
        double[] unfolded = point.clone();
        for (int step = index; step > 0; step--) {
            unfolded = hinged(path.get(step - 1), path.get(step), unfolded);
        }

        return unfolded;
    }

    private static double[] hinged(BoxSide onto, BoxSide from, double[] point) {
        double inward = onto.isNear() ? 1.0 : -1.0;
        double outward = from.isNear() ? -1.0 : 1.0;
        double[] hinged = point.clone();
        double depth = (point[onto.axis()] - onto.value()) * inward;
        hinged[onto.axis()] = onto.value();
        hinged[from.axis()] = from.value() + outward * depth;
        return hinged;
    }

    private double[][] edgeBetween(BoxSide first, BoxSide second) {
        int along = AXES - first.axis() - second.axis();
        double[] start = new double[AXES];
        start[first.axis()] = first.value();
        start[second.axis()] = second.value();
        double[] end = start.clone();
        end[along] = this.size[along];
        return new double[][] {start, end};
    }

    private static double[] flat(BoxSide side, double[] point) {
        return side.axis() == 0
                ? new double[] {point[1], point[2]}
                : new double[] {point[0], point[side.axis() == 1 ? 2 : 1]};
    }

    private static double crossingAt(double[] from, double[] to, double[] edgeFrom, double[] edgeTo) {
        double dx = to[0] - from[0];
        double dz = to[1] - from[1];
        double ex = edgeTo[0] - edgeFrom[0];
        double ez = edgeTo[1] - edgeFrom[1];
        double denominator = dx * ez - dz * ex;
        if (Math.abs(denominator) < ON_EDGE) {
            return Double.NaN;
        }

        double at = ((edgeFrom[0] - from[0]) * ez - (edgeFrom[1] - from[1]) * ex) / denominator;
        double along = ((edgeFrom[0] - from[0]) * dz - (edgeFrom[1] - from[1]) * dx) / denominator;
        boolean isOnBoth = at >= -ON_EDGE && at <= 1.0 + ON_EDGE && along >= -ON_EDGE && along <= 1.0 + ON_EDGE;
        return isOnBoth ? at : Double.NaN;
    }
}
