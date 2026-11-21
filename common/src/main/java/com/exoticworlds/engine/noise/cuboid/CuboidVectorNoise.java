package com.exoticworlds.engine.noise.cuboid;

final class CuboidVectorNoise {
    static float alongX(CuboidLattice.Face face, int alongHash, int acrossHash, int xNode, int zNode, float xOffset,
            float yOffset, float zOffset) {
        boolean onXEdge = CuboidNoiseSampler.onXEdge(face, xNode);
        boolean onZEdge = CuboidNoiseSampler.onZEdge(face, zNode);
        if (onXEdge && onZEdge) {
            return 0.0F;
        }

        if (onXEdge) {
            float across = CuboidNoiseSampler.row(face, acrossHash, xNode, zNode, xOffset, yOffset, zOffset);
            return (xNode == 0) == (face.xAcross() > 0.0) ? -across : across;
        }

        float along = CuboidNoiseSampler.row(face, alongHash, xNode, zNode, xOffset, yOffset, zOffset);
        return onZEdge ? face.xSign() * along : along;
    }

    static float alongZ(CuboidLattice.Face face, int alongHash, int acrossHash, int xNode, int zNode, float xOffset,
            float yOffset, float zOffset) {
        boolean onXEdge = CuboidNoiseSampler.onXEdge(face, xNode);
        boolean onZEdge = CuboidNoiseSampler.onZEdge(face, zNode);
        if (onXEdge && onZEdge) {
            return 0.0F;
        }

        if (onXEdge) {
            return face.zSign() * CuboidNoiseSampler.row(face, alongHash, xNode, zNode, xOffset, yOffset, zOffset);
        }

        float across = CuboidNoiseSampler.row(face, acrossHash, xNode, zNode, xOffset, yOffset, zOffset);
        return onZEdge && (zNode == 0) == (face.zAcross() > 0.0) ? -across : across;
    }

    private CuboidVectorNoise() {
    }
}
