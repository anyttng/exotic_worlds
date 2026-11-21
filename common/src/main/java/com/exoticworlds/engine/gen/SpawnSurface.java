package com.exoticworlds.engine.gen;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.cuboid.CuboidStraightWalk;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

final class SpawnSurface {
    private static final int NO_FACE = -1;

    private static final double CELL_CENTER = 0.5;

    private final WorldFold fold;

    private final List<CuboidFace> faces;

    private final CuboidStraightWalk walk;

    private SpawnSurface(WorldFold fold, CuboidAtlas atlas) {
        this.fold = fold;
        this.faces = atlas.faces();
        this.walk = CuboidStraightWalk.of(atlas);
    }

    static @Nullable SpawnSurface of(WorldFold fold) {
        CuboidAtlas atlas = fold.cuboidAtlas();
        return atlas == null ? null : new SpawnSurface(fold, atlas);
    }

    BlockPos walked(BlockPos searchOrigin, int blockX, int blockZ) {
        BlockPos.MutableBlockPos landed = new BlockPos.MutableBlockPos(blockX, searchOrigin.getY(), blockZ);
        int startFace = faceAt(searchOrigin.getX(), searchOrigin.getZ());
        if (startFace == NO_FACE) {
            return landed;
        }

        this.walk.land(startFace,
                searchOrigin.getX() + CELL_CENTER, searchOrigin.getZ() + CELL_CENTER,
                blockX + CELL_CENTER, blockZ + CELL_CENTER,
                (atlasFace, localX, localZ) -> {
                    CuboidFace face = this.faces.get(atlasFace);
                    landed.setX(face.blockX(Math.min(Mth.floor(localX), face.widthBlocks() - 1)));
                    landed.setZ(face.blockZ(Math.min(Mth.floor(localZ), face.depthBlocks() - 1)));
                });
        return landed;
    }

    BlockPos nearestTo(SpawnPoint point, int blockX, int blockZ) {
        return this.fold.nearestCopy(new BlockPos(point.blockX(), 0, point.blockZ()), new BlockPos(blockX, 0, blockZ));
    }

    private int faceAt(int blockX, int blockZ) {
        for (int face = 0; face < this.faces.size(); face++) {
            if (this.faces.get(face).contains(blockX, blockZ)) {
                return face;
            }
        }

        return NO_FACE;
    }
}
