package com.exoticworlds.engine.noise;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.cuboid.CuboidCellGrid;

public interface AquiferCellGrid {
    static AquiferCellGrid of(WorldFold fold, int vanillaCellWidth) {
        CuboidAtlas atlas = fold.cuboidAtlas();
        return atlas != null
                ? CuboidCellGrid.of(atlas, vanillaCellWidth)
                : TilingCellGrid.of(fold, vanillaCellWidth);
    }

    int cellOriginX(int blockX, int blockZ);

    int cellOriginZ(int blockX, int blockZ);

    GenerationFoldContext.Context.DivisorScope divide(GenerationFoldContext.Context context);
}
