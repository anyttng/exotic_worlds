package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public final class RelativeKeyFold {
    public static BlockPos shortWay(@Nullable Level level, BlockPos owner, Vec3i partner, BlockPos rawKey) {
        if (level == null) {
            return rawKey;
        }

        return shortWay(WorldLoopAttachments.wrappedFoldOfReader(level), owner, partner, rawKey);
    }

    static BlockPos shortWay(@Nullable WorldFold fold, BlockPos owner, Vec3i partner, BlockPos rawKey) {
        BlockPos partnerPos = new BlockPos(partner);
        BlockPos nearest = CreateSeamFold.nearest(fold, owner, partnerPos);
        if (nearest.equals(partnerPos)) {
            return rawKey;
        }

        return owner.subtract(nearest);
    }

    public static BlockPos normalize(@Nullable Level level, BlockPos owner, BlockPos storedKey) {
        if (level == null) {
            return storedKey;
        }

        return normalize(WorldLoopAttachments.wrappedFoldOfReader(level), owner, storedKey);
    }

    static BlockPos normalize(@Nullable WorldFold fold, BlockPos owner, BlockPos storedKey) {
        return shortWay(fold, owner, owner.subtract(storedKey), storedKey);
    }

    private RelativeKeyFold() {
    }
}
