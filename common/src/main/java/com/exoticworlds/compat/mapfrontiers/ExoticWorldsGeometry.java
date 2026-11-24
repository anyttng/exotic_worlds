package com.exoticworlds.compat.mapfrontiers;

import java.util.function.Predicate;

import com.exoticworlds.core.fold.RectangleCopies;
import com.exoticworlds.core.fold.WorldFold;

import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record ExoticWorldsGeometry(WorldFold fold) implements WorldGeometry {
    @Override
    public BlockPos nearestCopy(BlockPos reference, BlockPos target) {
        return this.fold.nearestCopy(reference, target);
    }

    @Override
    public BlockPos shortestDelta(BlockPos from, BlockPos to) {
        Vec3 delta = this.fold.foldDelta(Vec3.atLowerCornerOf(from), Vec3.atLowerCornerOf(to));
        return BlockPos.containing(delta.x, 0.0, delta.z);
    }

    @Override
    public int periodX() {
        return periodBlocks(Direction.Axis.X);
    }

    @Override
    public int periodZ() {
        return periodBlocks(Direction.Axis.Z);
    }

    @Override
    public ChunkPos foldChunk(ChunkPos chunk) {
        return this.fold.fold(chunk);
    }

    @Override
    public ChunkPos nearestChunkCopy(ChunkPos reference, ChunkPos target) {
        BlockPos from = new BlockPos(reference.getMinBlockX(), 0, reference.getMinBlockZ());
        BlockPos to = new BlockPos(target.getMinBlockX(), 0, target.getMinBlockZ());
        return this.fold.nearestCopyTransformation(from, to).apply(target);
    }

    @Override
    public boolean hasWrappedAxes() {
        return this.fold.isLooped(Direction.Axis.X) || this.fold.isLooped(Direction.Axis.Z);
    }

    public boolean isAnyCopyInBounds(BlockPos point, double minX, double maxX, double minZ, double maxZ,
            Predicate<BlockPos> test) {
        AABB bounds = new AABB(minX, point.getY(), minZ, Math.floor(maxX) + 1.0, point.getY() + 1.0,
                Math.floor(maxZ) + 1.0);
        for (WorldFold.Folded<BlockPos> copy : RectangleCopies.inside(this.fold, bounds, point, WorldFold.Folded::new)) {
            if (test.test(copy.value())) {
                return true;
            }
        }

        return false;
    }

    private int periodBlocks(Direction.Axis axis) {
        return this.fold.isDecomposedPerAxis() && this.fold.isLooped(axis) ? this.fold.getWidthBlocks(axis) : 0;
    }
}
