package com.exoticworlds.engine.noise;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.cuboid.CuboidCellGrid;

public interface AquiferCellGrid {
    static AquiferCellGrid of(WorldFold fold, int vanillaCellWidthBlocks) {
        CuboidAtlas atlas = fold.getCuboidAtlas();
        return atlas != null
                ? CuboidCellGrid.of(atlas, vanillaCellWidthBlocks)
                : TilingCellGrid.of(fold, vanillaCellWidthBlocks);
    }

    int cellOriginX(int blockX, int blockZ);

    int cellOriginZ(int blockX, int blockZ);

    GenerationFoldContext.Context.DivisorScope divide(GenerationFoldContext.Context context);
}
