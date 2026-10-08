package com.exoticworlds.engine.gen;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public record CanonicalRandomFactory(PositionalRandomFactory vanilla, WorldFold fold)
        implements PositionalRandomFactory {
    @Override
    public RandomSource fromHashOf(String name) {
        return this.vanilla.fromHashOf(name);
    }

    @Override
    public RandomSource fromSeed(long seed) {
        return this.vanilla.fromSeed(seed);
    }

    @Override
    public RandomSource at(int x, int y, int z) {
        long canonical = this.fold.foldBlockNode(BlockPos.asLong(x, y, z));
        return this.vanilla.at(BlockPos.getX(canonical), y, BlockPos.getZ(canonical));
    }

    @Override
    public void parityConfigString(StringBuilder builder) {
        this.vanilla.parityConfigString(builder);
    }
}
