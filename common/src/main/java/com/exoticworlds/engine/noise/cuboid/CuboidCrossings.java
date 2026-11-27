package com.exoticworlds.engine.noise.cuboid;

import java.util.List;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidEdge;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.SeamTransform;

import net.minecraft.core.Direction;

final class CuboidCrossings {
    static final int WEST = 0;
    static final int EAST = 1;
    static final int NORTH = 2;
    static final int SOUTH = 3;

    private static final int SIDES = 4;

    record Crossing(int face, SeamTransform transform) {
    }

    private final Crossing[][] crossings;

    private CuboidCrossings(Crossing[][] crossings) {
        this.crossings = crossings;
    }

    static CuboidCrossings of(CuboidAtlas atlas) {
        List<CuboidFace> faces = atlas.faces();
        Crossing[][] crossings = new Crossing[faces.size()][SIDES];
        for (CuboidEdge edge : atlas.edges()) {
            int first = indexOf(faces, edge.first());
            int second = indexOf(faces, edge.second());
            crossings[first][sideOf(edge.firstSide())] = new Crossing(second, edge.transform());
            crossings[second][sideOf(edge.secondSide())] = new Crossing(first, edge.transform().inverse());
        }

        return new CuboidCrossings(crossings);
    }

    Crossing at(int atlasFace, int side) {
        return this.crossings[atlasFace][side];
    }

    static int westOrEast(double x, int minBlockX) {
        return x < minBlockX ? WEST : EAST;
    }

    static int northOrSouth(double z, int minBlockZ) {
        return z < minBlockZ ? NORTH : SOUTH;
    }

    private static int indexOf(List<CuboidFace> faces, CuboidFace.Side side) {
        for (int i = 0; i < faces.size(); i++) {
            if (faces.get(i).side() == side) {
                return i;
            }
        }

        throw new IllegalArgumentException("The atlas has no " + side + " face");
    }

    private static int sideOf(Direction side) {
        return switch (side) {
            case WEST -> WEST;
            case EAST -> EAST;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            default -> throw new IllegalArgumentException("A face has no " + side + " edge");
        };
    }
}
