package com.exoticworlds.engine.noise.cuboid;

import com.exoticworlds.core.CuboidFace.BoxVector;
import com.exoticworlds.engine.noise.PerlinMath;

import net.minecraft.util.Mth;

final class CuboidNoiseSampler {
    enum Part {
        VALUE,
        SECOND_VALUE,
        WARP_X,
        WARP_Z
    }

    private static final int VERTEX_SIGN_BIT = 1;

    static float sample(Part part, CuboidLattice lattice, int atlasFace, double localX, double y, double localZ,
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
        float yFrac = fudgeYScale == PerlinMath.NO_FUDGE
                ? (float) yRelative
                : (float) (yRelative - PerlinMath.fudgeY(originalY, yRelative, fudgeYScale));
        byte[] permutations = lattice.permutations();
        float d000 = corner(part, permutations, face, xCell, yCell, zCell, xFrac, yFrac, zFrac);
        float d100 = corner(part, permutations, face, xCell + 1, yCell, zCell, xFrac - 1.0F, yFrac, zFrac);
        float d010 = corner(part, permutations, face, xCell, yCell + 1L, zCell, xFrac, yFrac - 1.0F, zFrac);
        float d110 = corner(part, permutations, face, xCell + 1, yCell + 1L, zCell, xFrac - 1.0F, yFrac - 1.0F, zFrac);
        float d001 = corner(part, permutations, face, xCell, yCell, zCell + 1, xFrac, yFrac, zFrac - 1.0F);
        float d101 = corner(part, permutations, face, xCell + 1, yCell, zCell + 1, xFrac - 1.0F, yFrac, zFrac - 1.0F);
        float d011 = corner(part, permutations, face, xCell, yCell + 1L, zCell + 1, xFrac, yFrac - 1.0F, zFrac - 1.0F);
        float d111 = corner(part, permutations, face, xCell + 1, yCell + 1L, zCell + 1, xFrac - 1.0F, yFrac - 1.0F,
                zFrac - 1.0F);
        return Mth.lerp3(Mth.smoothstep(xFrac), Mth.smoothstep((float) yRelative), Mth.smoothstep(zFrac),
                d000, d100, d010, d110, d001, d101, d011, d111);
    }

    private static float corner(Part part, byte[] permutations, CuboidLattice.Face face, int xNode, long yNode,
            int zNode, float xOffset, float yOffset, float zOffset) {
        int hash = hash(permutations, face, xNode, yNode, zNode);
        return switch (part) {
            case VALUE -> value(face, hash, xNode, zNode, xOffset, yOffset, zOffset);
            case SECOND_VALUE -> value(face, PerlinMath.p(permutations, hash), xNode, zNode, xOffset, yOffset,
                    zOffset);
            case WARP_X -> CuboidVectorNoise.alongX(face, hash, PerlinMath.p(permutations, hash), xNode, zNode,
                    xOffset, yOffset, zOffset);
            case WARP_Z -> CuboidVectorNoise.alongZ(face, hash, PerlinMath.p(permutations, hash), xNode, zNode,
                    xOffset, yOffset, zOffset);
        };
    }

    private static float value(CuboidLattice.Face face, int hash, int xNode, int zNode, float xOffset,
            float yOffset, float zOffset) {
        if (onXEdge(face, xNode) && onZEdge(face, zNode)) {
            return (hash & VERTEX_SIGN_BIT) == 0 ? yOffset : -yOffset;
        }

        return row(face, hash, xNode, zNode, xOffset, yOffset, zOffset);
    }

    static boolean onXEdge(CuboidLattice.Face face, int xNode) {
        return xNode == 0 || xNode == face.xCells();
    }

    static boolean onZEdge(CuboidLattice.Face face, int zNode) {
        return zNode == 0 || zNode == face.zCells();
    }

    static float row(CuboidLattice.Face face, int hash, int xNode, int zNode, float xOffset, float yOffset,
            float zOffset) {
        int[] gradient = PerlinMath.gradient(hash);
        if (onXEdge(face, xNode)) {
            double across = xNode == 0 ? -face.xAcross() : face.xAcross();
            return (float) (gradient[0] * zOffset * face.zSign() + gradient[1] * yOffset
                    + gradient[2] * xOffset * across);
        }

        if (onZEdge(face, zNode)) {
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
        return PerlinMath.p(permutations, PerlinMath.p(permutations,
                PerlinMath.p(permutations, PerlinMath.p(permutations, a) + b) + c) + yNode);
    }

    private CuboidNoiseSampler() {
    }
}
