package com.exoticworlds.engine.noise.cuboid;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.engine.noise.BoxDivisors;

import it.unimi.dsi.fastutil.ints.IntList;

public final class CuboidScaleCells {
    public static int shortestEdgeBlocks(CuboidAtlas atlas) {
        int shortest = Integer.MAX_VALUE;
        for (CuboidFace face : atlas.faces()) {
            shortest = Math.min(shortest, Math.min(face.widthBlocks(), face.depthBlocks()));
        }

        return shortest;
    }

    public static IntList perBoxAxis(CuboidAtlas atlas, double layerScale) {
        return IntList.of(CuboidLattice.cells(atlas, layerScale, BoxDivisors.UNDIVIDED));
    }

    public static long alongEdge(int edgeBlocks, double layerScale) {
        return CuboidLattice.cellsAlong(edgeBlocks, layerScale);
    }

    private CuboidScaleCells() {
    }
}
