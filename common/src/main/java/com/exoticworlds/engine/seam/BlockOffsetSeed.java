package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.RegionLevelSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.ObjectStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;

public final class BlockOffsetSeed implements AutoCloseable {
    private static final ThreadLocal<BlockOffsetSeed> CURRENT = ThreadLocal.withInitial(BlockOffsetSeed::new);

    private WorldFold fold = WorldFolds.NOOP;

    private final ObjectStack<WorldFold> previous = new ObjectStack<>();

    public static BlockPos seedPosition(BlockPos pos) {
        return CURRENT.get().fold.fold(pos);
    }

    public static BlockOffsetSeed seededBy(@Nullable BlockGetter getter) {
        BlockOffsetSeed scope = CURRENT.get();
        scope.push(foldOf(getter));
        return scope;
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

    private void push(WorldFold bound) {
        this.previous.push(this.fold);
        this.fold = bound;
    }

    @Override
    public void close() {
        this.fold = this.previous.pop();
    }

    private BlockOffsetSeed() {
    }
}
