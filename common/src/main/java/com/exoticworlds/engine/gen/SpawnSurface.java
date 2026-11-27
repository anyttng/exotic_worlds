package com.exoticworlds.engine.gen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.cuboid.CuboidStraightWalk;

import net.minecraft.core.BlockPos;

final class SpawnSurface {
    private final WorldFold fold;

    private final CuboidStraightWalk walk;

    private SpawnSurface(WorldFold fold, CuboidAtlas atlas) {
        this.fold = fold;
        this.walk = CuboidStraightWalk.of(atlas);
    }

    static @Nullable SpawnSurface of(WorldFold fold) {
        CuboidAtlas atlas = fold.getCuboidAtlas();
        return atlas == null ? null : new SpawnSurface(fold, atlas);
    }

    BlockPos walked(BlockPos searchOrigin, int blockX, int blockZ) {
        return this.walk.landedBlock(searchOrigin, blockX, blockZ);
    }

    BlockPos nearestTo(SpawnPoint point, int blockX, int blockZ) {
        return this.fold.nearestCopy(new BlockPos(point.getBlockX(), 0, point.getBlockZ()), new BlockPos(blockX, 0, blockZ));
    }
}
