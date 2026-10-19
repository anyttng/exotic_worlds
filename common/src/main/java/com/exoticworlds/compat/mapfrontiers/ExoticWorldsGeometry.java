package com.exoticworlds.compat.mapfrontiers;

import java.util.function.Predicate;

import com.exoticworlds.api.v1.ToroidalShape;

import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record ExoticWorldsGeometry(ToroidalShape shape) implements WorldGeometry {
    @Override
    public BlockPos nearestCopy(BlockPos reference, BlockPos target) {
        return this.shape.nearestCopy(reference, target);
    }

    @Override
    public BlockPos shortestDelta(BlockPos from, BlockPos to) {
        Vec3 delta = this.shape.shortestDelta(Vec3.atLowerCornerOf(from), Vec3.atLowerCornerOf(to));
        return BlockPos.containing(delta.x, 0.0, delta.z);
    }

    @Override
    public int periodX() {
        return period(Direction.Axis.X);
    }

    @Override
    public int periodZ() {
        return period(Direction.Axis.Z);
    }

    @Override
    public ChunkPos foldChunk(ChunkPos chunk) {
        return this.shape.fold(chunk);
    }

    @Override
    public ChunkPos nearestChunkCopy(ChunkPos reference, ChunkPos target) {
        BlockPos from = new BlockPos(reference.getMinBlockX(), 0, reference.getMinBlockZ());
        BlockPos to = new BlockPos(target.getMinBlockX(), 0, target.getMinBlockZ());
        return this.shape.shiftToNearestCopy(from, to).apply(target);
    }

    @Override
    public boolean hasWrappedAxes() {
        return this.shape.loops(Direction.Axis.X) || this.shape.loops(Direction.Axis.Z);
    }

    public boolean anyCopyInBounds(BlockPos point, double minX, double maxX, double minZ, double maxZ,
            Predicate<BlockPos> test) {
        AABB bounds = new AABB(minX, point.getY(), minZ, Math.floor(maxX) + 1.0, point.getY() + 1.0,
                Math.floor(maxZ) + 1.0);
        for (ToroidalShape.Oriented<BlockPos> copy : this.shape.copiesInside(bounds, point)) {
            if (test.test(copy.value())) {
                return true;
            }
        }

        return false;
    }

    private int period(Direction.Axis axis) {
        return this.shape.decomposesPerAxis() && this.shape.loops(axis) ? this.shape.widthBlocks(axis) : 0;
    }
}
