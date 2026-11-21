package com.exoticworlds.engine.noise;

import com.exoticworlds.core.CuboidFace.BoxVector;

import net.minecraft.util.Mth;

final class CuboidNoiseSampler {
    private static final int GRADIENT_MASK = 15;
    private static final int VERTEX_SIGN_BIT = 1;

    static float sample(CuboidLattice lattice, int atlasFace, double localX, double y, double localZ,
            double originalY) {
        CuboidLattice.Face face = lattice.faces()[atlasFace];
        double xs = localX * face.xRatio();
        double ys = y + lattice.yOffset();
        double zs = localZ * face.zRatio();
        int xCell = Mth.clamp(Mth.floor(xs), 0, face.xCells() - 1);
        long yCell = Mth.lfloor(ys);
        int zCell = Mth.clamp(Mth.floor(zs), 0, face.zCells() - 1);
        float xFrac = (float) (xs - xCell);
        double yRelative = ys - yCell;
        float zFrac = (float) (zs - zCell);
        double fudgeYScale = lattice.fudgeYScale();
        float yFrac = fudgeYScale == PeriodicNoiseSampler.NO_FUDGE
                ? (float) yRelative
                : (float) (yRelative - PeriodicNoiseSampler.fudgeY(originalY, yRelative, fudgeYScale));
        byte[] permutations = lattice.permutations();
        float d000 = corner(permutations, face, xCell, yCell, zCell, xFrac, yFrac, zFrac);
        float d100 = corner(permutations, face, xCell + 1, yCell, zCell, xFrac - 1.0F, yFrac, zFrac);
        float d010 = corner(permutations, face, xCell, yCell + 1L, zCell, xFrac, yFrac - 1.0F, zFrac);
        float d110 = corner(permutations, face, xCell + 1, yCell + 1L, zCell, xFrac - 1.0F, yFrac - 1.0F, zFrac);
        float d001 = corner(permutations, face, xCell, yCell, zCell + 1, xFrac, yFrac, zFrac - 1.0F);
        float d101 = corner(permutations, face, xCell + 1, yCell, zCell + 1, xFrac - 1.0F, yFrac, zFrac - 1.0F);
        float d011 = corner(permutations, face, xCell, yCell + 1L, zCell + 1, xFrac, yFrac - 1.0F, zFrac - 1.0F);
        float d111 = corner(permutations, face, xCell + 1, yCell + 1L, zCell + 1, xFrac - 1.0F, yFrac - 1.0F,
                zFrac - 1.0F);
        return Mth.lerp3(Mth.smoothstep(xFrac), Mth.smoothstep((float) yRelative), Mth.smoothstep(zFrac),
                d000, d100, d010, d110, d001, d101, d011, d111);
    }

    private static float corner(byte[] permutations, CuboidLattice.Face face, int xNode, long yNode, int zNode,
            float xOffset, float yOffset, float zOffset) {
        int hash = hash(permutations, face, xNode, yNode, zNode);
        boolean onXEdge = xNode == 0 || xNode == face.xCells();
        boolean onZEdge = zNode == 0 || zNode == face.zCells();
        if (onXEdge && onZEdge) {
            return (hash & VERTEX_SIGN_BIT) == 0 ? yOffset : -yOffset;
        }

        int[] gradient = PeriodicNoiseSampler.GRADIENT[hash & GRADIENT_MASK];
        if (onXEdge) {
            double across = xNode == 0 ? -face.xAcross() : face.xAcross();
            return (float) (gradient[0] * zOffset * face.zSign() + gradient[1] * yOffset
                    + gradient[2] * xOffset * across);
        }

        if (onZEdge) {
            double across = zNode == 0 ? -face.zAcross() : face.zAcross();
            return (float) (gradient[0] * xOffset * face.xSign() + gradient[1] * yOffset
                    + gradient[2] * zOffset * across);
        }

        return gradient[0] * xOffset + gradient[1] * yOffset + gradient[2] * zOffset;
    }

    private static int hash(byte[] permutations, CuboidLattice.Face face, int xNode, long yNode, int zNode) {
        BoxVector origin = face.origin();
        BoxVector alongX = face.alongX();
        BoxVector alongZ = face.alongZ();
        int a = origin.a() + xNode * alongX.a() + zNode * alongZ.a();
        int b = origin.b() + xNode * alongX.b() + zNode * alongZ.b();
        int c = origin.c() + xNode * alongX.c() + zNode * alongZ.c();
        return PeriodicNoiseSampler.p(permutations, PeriodicNoiseSampler.p(permutations,
                PeriodicNoiseSampler.p(permutations, PeriodicNoiseSampler.p(permutations, a) + b) + c) + yNode);
    }

    private CuboidNoiseSampler() {
    }
}
