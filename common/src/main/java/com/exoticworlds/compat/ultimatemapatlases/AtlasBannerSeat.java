package com.exoticworlds.compat.ultimatemapatlases;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.MapSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class AtlasBannerSeat {
    public static BlockPos toward(MapItemSavedData data, int x, int z, BlockPos banner) {
        return toward(MapSeamFold.transformerFor(null, data.dimension), x, z, banner);
    }

    static BlockPos toward(@Nullable WorldFold fold, int x, int z, BlockPos banner) {
        return NearestCopy.toward(fold, new BlockPos(x, banner.getY(), z), banner);
    }

    private AtlasBannerSeat() {
    }
}
