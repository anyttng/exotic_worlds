package com.exoticworlds.engine.noise.cuboid;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.CuboidFace.BoxVector;
import com.exoticworlds.engine.noise.AquiferCellGrid;
import com.exoticworlds.engine.noise.BoxDivisors;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.TilingCellGrid;
import com.exoticworlds.engine.noise.cuboid.CuboidCrossings.Crossing;

public record CuboidCellGrid(CuboidAtlas atlas, int aCellWidth, int bCellWidth, int cCellWidth,
        CuboidAskingFace asking) implements AquiferCellGrid {
    public static CuboidCellGrid of(CuboidAtlas atlas, int vanillaCellWidth) {
        int[] widths = new int[CuboidLattice.AXES];
        for (CuboidFace face : atlas.faces()) {
            widths[CuboidLattice.axisOf(face.alongX())] =
                    TilingCellGrid.tilingWidth(face.widthBlocks(), vanillaCellWidth);
            widths[CuboidLattice.axisOf(face.alongZ())] =
                    TilingCellGrid.tilingWidth(face.depthBlocks(), vanillaCellWidth);
        }

        return new CuboidCellGrid(atlas, widths[CuboidLattice.A_AXIS], widths[CuboidLattice.B_AXIS],
                widths[CuboidLattice.C_AXIS], CuboidAskingFace.of(atlas));
    }

    @Override
    public int cellOriginX(int blockX, int blockZ) {
        CuboidFace face = this.atlas.faceAt(blockX, blockZ);
        if (face == null) {
            Crossing crossing = this.asking.crossingOf(blockX, blockZ);
            return originOnFaceX(this.atlas.faces().get(crossing.face()),
                    crossing.transform().applyCellX(blockX, blockZ));
        }

        return originOnFaceX(face, blockX);
    }

    @Override
    public int cellOriginZ(int blockX, int blockZ) {
        CuboidFace face = this.atlas.faceAt(blockX, blockZ);
        if (face == null) {
            Crossing crossing = this.asking.crossingOf(blockX, blockZ);
            return originOnFaceZ(this.atlas.faces().get(crossing.face()),
                    crossing.transform().applyCellZ(blockX, blockZ));
        }

        return originOnFaceZ(face, blockZ);
    }

    @Override
    public GenerationFoldContext.Context.DivisorScope divide(GenerationFoldContext.Context context) {
        return context.withBoxDivisors(new BoxDivisors(this.aCellWidth, this.bCellWidth, this.cCellWidth));
    }

    public int xCellWidth(CuboidFace face) {
        return cellWidth(face.alongX());
    }

    public int zCellWidth(CuboidFace face) {
        return cellWidth(face.alongZ());
    }

    private int cellWidth(BoxVector direction) {
        return switch (CuboidLattice.axisOf(direction)) {
            case CuboidLattice.A_AXIS -> this.aCellWidth;
            case CuboidLattice.B_AXIS -> this.bCellWidth;
            default -> this.cCellWidth;
        };
    }

    private int originOnFaceX(CuboidFace face, int blockX) {
        int width = xCellWidth(face);
        return face.blockX(Math.floorDiv(face.localX(blockX), width) * width);
    }

    private int originOnFaceZ(CuboidFace face, int blockZ) {
        int width = zCellWidth(face);
        return face.blockZ(Math.floorDiv(face.localZ(blockZ), width) * width);
    }
}
