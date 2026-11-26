package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.RegionLevelSource;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.ObjectStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BlockOffsetSeed {
    private static final ThreadLocal<BlockOffsetSeed> CURRENT = ThreadLocal.withInitial(BlockOffsetSeed::new);

    private WorldFold fold = WorldFolds.NOOP;

    private final ObjectStack<WorldFold> previous = new ObjectStack<>();

    public static BlockPos seedPosition(BlockPos pos) {
        return CURRENT.get().fold.fold(pos);
    }

    public static Vec3 inCopyFrame(BlockPos pos, Vec3 canonicalOffset) {
        return inCopyFrame(CURRENT.get().fold, pos, canonicalOffset);
    }

    static Vec3 inCopyFrame(WorldFold fold, BlockPos pos, Vec3 canonicalOffset) {
        if (fold.isPreservingLocalIndices()) {
            return canonicalOffset;
        }

        return fold.foldOriented(pos).orientation().inverse().applyToDelta(canonicalOffset);
    }

    public static @Nullable BlockOffsetSeed enter(@Nullable BlockGetter getter, BlockState state) {
        if (!state.hasOffsetFunction()) {
            return null;
        }

        BlockOffsetSeed scope = CURRENT.get();
        scope.push(getFold(getter));
        return scope;
    }

    public static void exit(@Nullable BlockOffsetSeed scope) {
        if (scope != null) {
            scope.pop();
        }
    }

    private static WorldFold getFold(@Nullable BlockGetter getter) {
        if (getter instanceof LevelReader reader) {
            return WorldLoopAttachments.getFoldOfReader(reader);
        }

        if (getter instanceof RegionLevelSource region) {
            return WorldLoopAttachments.getFoldOfReader(region.exotic_worlds$getRegionLevel());
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
