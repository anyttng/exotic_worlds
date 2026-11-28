package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.seam.MapSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CreateSeamFold {
    public static @Nullable WorldFold getWrappedFold(@Nullable Level level, @Nullable ResourceKey<Level> dimension) {
        if (level != null) {
            return WorldLoopAttachments.getWrappedFoldOfReader(level);
        }

        return dimension == null ? null : MapSeamFold.getWrappedFold(null, dimension);
    }

    public static Vec3 nearestCopy(@Nullable ResourceKey<Level> dimension, Vec3 anchor, Vec3 target) {
        return NearestCopy.toward(getWrappedFold(null, dimension), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable Level level, @Nullable ResourceKey<Level> dimension, Vec3 anchor,
            Vec3 target) {
        return NearestCopy.toward(getWrappedFold(level, dimension), anchor, target);
    }

    public static Vec3i foldPositionToBox(@Nullable Level level, BoundingBox box, Vec3i position) {
        return NearestCopy.toward(WorldLoopAttachments.getWrappedFoldOfReader(level), box, position);
    }

    public static BlockPos foldPositionToBox(@Nullable Level level, BoundingBox box, BlockPos position) {
        return NearestCopy.toward(WorldLoopAttachments.getWrappedFoldOfReader(level), box, position);
    }

    public static Vec3 inFrameOf(WorldFold fold, Vec3 viewer, Vec3 anchor, Vec3 point) {
        return CreateFrameSeat.of(fold, viewer, anchor).apply(point);
    }

    public static BlockPos foldDelta(@Nullable Level level, Vec3i anchor, BlockPos target, BlockPos rawDelta) {
        if (level == null) {
            return rawDelta;
        }

        return delta(WorldLoopAttachments.getWrappedFoldOfReader(level), NearestCopy.asBlockPos(anchor), target, rawDelta);
    }

    public static BlockPos farEndDelta(@Nullable Level level, BlockPos anchor, BlockPos delta, BlockPos rawFarEnd) {
        if (level == null) {
            return rawFarEnd;
        }

        return farEndDelta(WorldLoopAttachments.getFoldOfReader(level), anchor, delta, rawFarEnd);
    }

    public static BlockPos nearestCopy(@Nullable Level level, Vec3i anchor, BlockPos target) {
        return nearest(WorldLoopAttachments.getWrappedFoldOfReader(level), NearestCopy.asBlockPos(anchor), target);
    }

    public static Vec3 nearestCopy(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return NearestCopy.toward(WorldLoopAttachments.getWrappedFoldOfReader(level), anchor, target);
    }

    public static Vec3 foldPointToBox(@Nullable Level level, AABB box, Vec3 point) {
        return NearestCopy.toward(WorldLoopAttachments.getWrappedFoldOfReader(level), box.getCenter(), point);
    }

    public static DeckTransformation nearestCopyTransformation(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return NearestCopy.transformationToward(WorldLoopAttachments.getWrappedFoldOfReader(level), anchor, target);
    }

    public static BlockPos canonical(@Nullable ServerLevel level, BlockPos position) {
        return WorldLoopAttachments.getFoldOfReader(level).fold(position);
    }

    public static BlockPos canonical(@Nullable ResourceKey<Level> dimension, BlockPos position) {
        WorldFold fold = getWrappedFold(null, dimension);
        return (fold == null ? WorldFolds.NOOP : fold).fold(position);
    }

    public static BlockPos canonicalOnServer(@Nullable BlockGetter level, BlockPos position) {
        return level instanceof ServerLevel serverLevel ? canonical(serverLevel, position) : position;
    }

    public static Vec3 canonicalOnServer(@Nullable BlockGetter level, Vec3 position) {
        return level instanceof ServerLevel serverLevel
                ? WorldLoopAttachments.getFoldOfReader(serverLevel).fold(position)
                : position;
    }

    // Off the server thread the write is the viewer's frame, and canonicalizing it jumps the carriage a world width.
    public static Vec3 canonicalOnServer(@Nullable ResourceKey<Level> dimension, Vec3 position) {
        MinecraftServer server = CurrentServer.get();
        if (server == null || !server.isSameThread() || dimension == null) {
            return position;
        }

        return WorldLoopAttachments.getFoldIfLevel(server.getLevel(dimension)).fold(position);
    }

    static BlockPos delta(@Nullable WorldFold fold, BlockPos anchor, BlockPos target, BlockPos rawDelta) {
        BlockPos nearest = nearest(fold, anchor, target);
        if (nearest.equals(target)) {
            return rawDelta;
        }

        return nearest.subtract(anchor);
    }

    static BlockPos farEndDelta(WorldFold fold, BlockPos anchor, BlockPos delta, BlockPos rawFarEnd) {
        BlockPos farEnd = fold.fold(anchor.offset(delta));
        BlockPos nearestAnchor = nearest(fold, farEnd, anchor);
        if (nearestAnchor.equals(anchor)) {
            return rawFarEnd;
        }

        return nearestAnchor.subtract(farEnd);
    }

    static BlockPos nearest(@Nullable WorldFold fold, BlockPos anchor, BlockPos target) {
        return NearestCopy.toward(fold, anchor, target);
    }

    private CreateSeamFold() {
    }
}
