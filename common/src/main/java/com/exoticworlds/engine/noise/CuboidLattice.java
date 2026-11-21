package com.exoticworlds.engine.noise;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.CuboidFace.BoxVector;

@SuppressWarnings("ArrayRecordComponent")
record CuboidLattice(byte[] permutations, double yOffset, double fudgeYScale, Face[] faces) {
    private static final int AXES = 3;
    private static final int A_AXIS = 0;
    private static final int B_AXIS = 1;
    private static final int C_AXIS = 2;

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
        int[] cells = new int[AXES];
        for (CuboidFace face : atlas.faces()) {
            cells[axisOf(face.alongX())] = cellsAlong(face.widthBlocks(), scale);
            cells[axisOf(face.alongZ())] = cellsAlong(face.depthBlocks(), scale);
        }

        Face[] faces = new Face[atlas.faces().size()];
        for (int i = 0; i < faces.length; i++) {
            faces[i] = Face.of(atlas.faces().get(i), cells);
        }

        return new CuboidLattice(permutations, yOffset, fudgeYScale, faces);
    }

    static int cellsAlong(int blocks, double scale) {
        return (int) Math.max(1L, Math.round(blocks * scale));
    }

    private static int inCells(int originBlocks, int cells) {
        return originBlocks == 0 ? 0 : cells;
    }

    private static int axisOf(BoxVector direction) {
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
