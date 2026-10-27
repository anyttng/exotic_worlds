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
import net.minecraft.world.level.block.state.BlockState;

public final class BlockOffsetSeed {
    private static final ThreadLocal<BlockOffsetSeed> CURRENT = ThreadLocal.withInitial(BlockOffsetSeed::new);

    private WorldFold fold = WorldFolds.NOOP;

    private final ObjectStack<WorldFold> previous = new ObjectStack<>();

    public static BlockPos seedPosition(BlockPos pos) {
        return CURRENT.get().fold.fold(pos);
    }

    public static @Nullable BlockOffsetSeed enter(@Nullable BlockGetter getter, BlockState state) {
        if (!state.hasOffsetFunction()) {
            return null;
        }

        BlockOffsetSeed scope = CURRENT.get();
        scope.push(foldOf(getter));
        return scope;
    }

    public static void exit(@Nullable BlockOffsetSeed scope) {
        if (scope != null) {
            scope.pop();
        }
    }

    private static WorldFold foldOf(@Nullable BlockGetter getter) {
        if (getter instanceof LevelReader reader) {
            return WorldLoopAttachments.transformerOfReader(reader);
        }

        if (getter instanceof RegionLevelSource region) {
            return WorldLoopAttachments.transformerOfReader(region.exotic_worlds$regionLevel());
        }

        return WorldFolds.NOOP;
    }

    private void push(WorldFold bound) {
        this.previous.push(this.fold);
        this.fold = bound;
    }

    private void pop() {
        this.fold = this.previous.pop();
    }

    private BlockOffsetSeed() {
    }
}
