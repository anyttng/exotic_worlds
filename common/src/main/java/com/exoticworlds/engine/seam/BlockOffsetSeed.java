package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.RegionLevelSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;

public final class BlockOffsetSeed {
    public static BlockPos seedPosition(@Nullable BlockGetter getter, BlockPos pos) {
        return foldOf(getter).fold(pos);
    }

    private static WorldFold foldOf(@Nullable BlockGetter getter) {
        if (getter instanceof LevelReader reader) {
            return WorldLoopAttachments.transformerOfReader(reader);
        }

        if (getter instanceof RegionLevelSource region) {
            return WorldLoopAttachments.transformerOfReader(region.toroidal$regionLevel());
        }

        return WorldFolds.NOOP;
    }

    private BlockOffsetSeed() {
    }
}
