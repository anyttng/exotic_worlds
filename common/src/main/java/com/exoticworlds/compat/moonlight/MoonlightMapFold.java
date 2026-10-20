package com.exoticworlds.compat.moonlight;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.PublishedShapes;
import com.exoticworlds.engine.seam.MapSeamFold;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class MoonlightMapFold {
    public static @Nullable WorldFold of(@Nullable LevelAccessor level, MapItemSavedData data) {
        WorldFold fold = MapSeamFold.transformerFor(level, data.dimension);
        return fold != null ? fold : PublishedShapes.foldOf(data.dimension);
    }

    private MoonlightMapFold() {
    }
}
