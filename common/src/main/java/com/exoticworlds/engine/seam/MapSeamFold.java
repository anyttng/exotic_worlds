package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.level.PublishedShapes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public final class MapSeamFold {
    public static @Nullable WorldFold transformerFor(@Nullable LevelAccessor level, ResourceKey<Level> dimension) {
        if (level instanceof Level actualLevel && !actualLevel.isClientSide()) {
            return WorldLoopAttachments.wrappedTransformerOf(actualLevel);
        }

        WorldFold serverFold = WorldLoopAttachments.wrappedTransformerOf(CurrentServer.get(), dimension);
        return serverFold != null ? serverFold : PublishedShapes.foldOf(dimension);
    }

    private MapSeamFold() {
    }
}
