package com.exoticworlds.compat.apprenticecodex;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.apprenticecodex.mixin.BlockTargetDataAccessor;
import com.exoticworlds.core.WorldFold;

import jp.aquafactory.apprenticecodex.utility.BlockTargetData;

import net.minecraft.world.phys.Vec3;

final class BlockTargetFold {
    static @Nullable BlockTargetData toServer(@Nullable BlockTargetData data, WorldFold fold) {
        if (data == null) {
            return null;
        }

        BlockTargetDataAccessor access = (BlockTargetDataAccessor) (Object) data;
        if (!access.exotic_worlds$hasTarget()) {
            return data;
        }

        BlockTarget target = new BlockTarget(access.exotic_worlds$getHitBlockPos(), access.exotic_worlds$getHitFace(),
                new Vec3(access.exotic_worlds$getHitX(), access.exotic_worlds$getHitY(), access.exotic_worlds$getHitZ()),
                access.exotic_worlds$getPlacePos(), access.exotic_worlds$getPlaceFacing());
        BlockTarget folded = target.toServer(fold);
        if (folded == target) {
            return data;
        }

        BlockTargetData written = BlockTargetDataAccessor.exotic_worlds$create();
        ((BlockTargetDataAccessor) (Object) written).exotic_worlds$setTarget(folded.hit(), folded.hitFace(),
                folded.hitLocation(), folded.place(), folded.placeFacing());
        return written;
    }

    static List<BlockTargetData> eachToServer(List<BlockTargetData> targets, WorldFold fold) {
        List<BlockTargetData> folded = new ArrayList<>(targets.size());
        boolean isMoved = false;
        for (BlockTargetData target : targets) {
            BlockTargetData foldedTarget = toServer(target, fold);
            isMoved |= foldedTarget != target;
            folded.add(foldedTarget);
        }

        return isMoved ? folded : targets;
    }

    private BlockTargetFold() {
    }
}
