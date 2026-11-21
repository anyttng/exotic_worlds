package com.exoticworlds.engine.noise.cuboid;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.CuboidFace.BoxVector;
import com.exoticworlds.engine.noise.BoxDivisors;

@SuppressWarnings("ArrayRecordComponent")
record CuboidLattice(byte[] permutations, double yOffset, double fudgeYScale, Face[] faces) {
    static final int AXES = 3;
    static final int A_AXIS = 0;
    static final int B_AXIS = 1;
    static final int C_AXIS = 2;

    record Face(int xCells, int zCells, double xRatio, double zRatio, BoxVector origin, BoxVector alongX,
            BoxVector alongZ, int xSign, int zSign, double xAcross, double zAcross) {
        private static Face of(CuboidFace face, int[] cells) {
            int xAxis = axisOf(face.alongX());
            int zAxis = axisOf(face.alongZ());
            int normal = AXES - xAxis - zAxis;
            int xCells = cells[xAxis];
            int zCells = cells[zAxis];
            double xCellBlocks = (double) face.widthBlocks() / xCells;
            double zCellBlocks = (double) face.depthBlocks() / zCells;
            return new Face(xCells, zCells, 1.0 / xCellBlocks, 1.0 / zCellBlocks,
                    new BoxVector(inCells(face.origin().a(), cells[A_AXIS]),
                            inCells(face.origin().b(), cells[B_AXIS]),
                            inCells(face.origin().c(), cells[C_AXIS])),
                    face.alongX(), face.alongZ(), component(face.alongX(), xAxis), component(face.alongZ(), zAxis),
                    (normal < xAxis ? 1 : -1) * xCellBlocks / zCellBlocks,
                    (normal < zAxis ? 1 : -1) * zCellBlocks / xCellBlocks);
        }
    }

    static CuboidLattice of(byte[] permutations, double yOffset, double fudgeYScale, CuboidAtlas atlas,
            double scale) {
        return of(permutations, yOffset, fudgeYScale, atlas, scale, BoxDivisors.UNDIVIDED);
    }

    static CuboidLattice of(byte[] permutations, double yOffset, double fudgeYScale, CuboidAtlas atlas,
            double scale, BoxDivisors divisors) {
        int[] cells = cells(atlas, scale, divisors);
        Face[] faces = new Face[atlas.faces().size()];
        for (int i = 0; i < faces.length; i++) {
            faces[i] = Face.of(atlas.faces().get(i), cells);
        }

        return new CuboidLattice(permutations, yOffset, fudgeYScale, faces);
    }

    static int[] cells(CuboidAtlas atlas, double scale, BoxDivisors divisors) {
        int[] cells = new int[AXES];
        for (CuboidFace face : atlas.faces()) {
            int xAxis = axisOf(face.alongX());
            int zAxis = axisOf(face.alongZ());
            cells[xAxis] = cellsAlong(face.widthBlocks(), scale / divisor(divisors, xAxis));
            cells[zAxis] = cellsAlong(face.depthBlocks(), scale / divisor(divisors, zAxis));
        }

        return cells;
    }

    static int cellsAlong(int blocks, double scale) {
        return (int) Math.max(1L, Math.round(blocks * scale));
    }

    private static int inCells(int originBlocks, int cells) {
        return originBlocks == 0 ? 0 : cells;
    }

    private static double divisor(BoxDivisors divisors, int axis) {
        return switch (axis) {
            case A_AXIS -> divisors.a();
            case B_AXIS -> divisors.b();
            default -> divisors.c();
        };
    }

    static int axisOf(BoxVector direction) {
        if (direction.a() != 0) {
            return A_AXIS;
        }

        return direction.b() != 0 ? B_AXIS : C_AXIS;
    }

    private static int component(BoxVector direction, int axis) {
        return switch (axis) {
            case A_AXIS -> direction.a();
            case B_AXIS -> direction.b();
            default -> direction.c();
        };
    }
}
