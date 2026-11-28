package com.exoticworlds.compat.mapatlases;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.MapSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

public final class BannerDecorationSeat {
    public static BlockPos towardMapCenter(MapItemSavedData data, BlockPos banner) {
        return towardMapCenter(MapSeamFold.getWrappedFold(null, data.dimension), data.centerX, data.centerZ, banner);
    }

    static BlockPos towardMapCenter(@Nullable WorldFold fold, int centerX, int centerZ, BlockPos banner) {
        return NearestCopy.transformationToward(fold, new Vec3(centerX, 0.0, centerZ), Vec3.atBottomCenterOf(banner))
                .apply(banner);
    }

    private BannerDecorationSeat() {
    }
}
