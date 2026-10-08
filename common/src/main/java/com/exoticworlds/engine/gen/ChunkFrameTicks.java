package com.exoticworlds.engine.gen;

import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;

public final class ChunkFrameTicks<T> implements LevelTickAccess<T> {
    private final LevelTickAccess<T> ticks;
    private final UnaryOperator<BlockPos> inChunkFrame;

    public ChunkFrameTicks(LevelTickAccess<T> ticks, UnaryOperator<BlockPos> inChunkFrame) {
        this.ticks = ticks;
        this.inChunkFrame = inChunkFrame;
    }

    @Override
    public void schedule(ScheduledTick<T> tick) {
        BlockPos pos = this.inChunkFrame.apply(tick.pos());
        this.ticks.schedule(pos.equals(tick.pos())
                ? tick
                : new ScheduledTick<>(tick.type(), pos, tick.triggerTick(), tick.priority(), tick.subTickOrder()));
    }

    @Override
    public boolean hasScheduledTick(BlockPos pos, T type) {
        return this.ticks.hasScheduledTick(this.inChunkFrame.apply(pos), type);
    }

    @Override
    public boolean willTickThisTick(BlockPos pos, T type) {
        return this.ticks.willTickThisTick(this.inChunkFrame.apply(pos), type);
    }

    @Override
    public int count() {
        return this.ticks.count();
    }
}
