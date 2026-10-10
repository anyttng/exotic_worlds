package com.exoticworlds.compat.electroenergetics;

import java.util.List;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public final class BoxCopies {
    private static final int BOX_HEIGHT = 1;

    public static ChunkPos seat(WorldFold fold, ChunkPos centre, int radius, ChunkPos chunk) {
        return new ChunkPos(seat(fold, square(centre, radius), middle(centre), middle(chunk)));
    }

    public static int count(WorldFold fold, ChunkPos centre, int radius, ChunkPos chunk) {
        return copies(fold, square(centre, radius), middle(chunk)).size();
    }

    public static BlockPos seat(WorldFold fold, BlockPos origin, Vec3i size, BlockPos pos) {
        AABB box = new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + size.getX(),
                origin.getY() + size.getY(), origin.getZ() + size.getZ());
        return seat(fold, box, origin.offset(size.getX() / 2, size.getY() / 2, size.getZ() / 2), pos);
    }

    private static BlockPos seat(WorldFold fold, AABB box, BlockPos centre, BlockPos pos) {
        BlockPos nearest = fold.nearestCopy(centre, pos);
        if (inside(box, nearest)) {
            return nearest;
        }

        BlockPos seated = nearest;
        long seatedDistance = Long.MAX_VALUE;
        for (ToroidalShape.Oriented<BlockPos> copy : copies(fold, box, pos)) {
            long distance = horizontalDistance(centre, copy.value());
            if (distance < seatedDistance) {
                seated = copy.value();
                seatedDistance = distance;
            }
        }

        return seated;
    }

    private static List<ToroidalShape.Oriented<BlockPos>> copies(WorldFold fold, AABB box, BlockPos pos) {
        return new ToroidalShapeView(fold).copiesInside(box, pos);
    }

    private static AABB square(ChunkPos centre, int radius) {
        return new AABB(blockOf(centre.x - radius), 0, blockOf(centre.z - radius), blockOf(centre.x + radius + 1),
                BOX_HEIGHT, blockOf(centre.z + radius + 1));
    }

    private static BlockPos middle(ChunkPos chunk) {
        return new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ());
    }

    private static int blockOf(int chunk) {
        return chunk * CoordinateConstants.CHUNK_WIDTH;
    }

    private static boolean inside(AABB box, BlockPos pos) {
        return pos.getX() >= box.minX && pos.getX() < box.maxX && pos.getZ() >= box.minZ && pos.getZ() < box.maxZ;
    }

    private static long horizontalDistance(BlockPos from, BlockPos to) {
        long dx = (long) to.getX() - from.getX();
        long dz = (long) to.getZ() - from.getZ();
        return dx * dx + dz * dz;
    }

    private BoxCopies() {
    }
}
