package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.ForeignFrames;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.level.CurrentClientLevel;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CreateSeamFold {
    public static @Nullable WorldFold wrappedFoldOf(@Nullable Level level,
            @Nullable ResourceKey<Level> dimension) {
        WorldFold fold = foldOf(level, dimension);
        return fold.isWrapped() ? fold : null;
    }

    public static WorldFold foldOf(@Nullable Level level, @Nullable ResourceKey<Level> dimension) {
        if (level != null) {
            return WorldLoopAttachments.foldOfReader(level);
        }

        if (dimension == null) {
            return WorldFolds.NOOP;
        }

        MinecraftServer server = CurrentServer.get();
        if (server == null) {
            return clientFoldOf(dimension);
        }

        return WorldLoopAttachments.foldIfLevel(server.getLevel(dimension));
    }

    private static WorldFold clientFoldOf(ResourceKey<Level> dimension) {
        Level clientLevel = CurrentClientLevel.get();
        if (clientLevel == null || !clientLevel.dimension().equals(dimension)) {
            return WorldFolds.NOOP;
        }

        return WorldLoopAttachments.foldOfReader(clientLevel);
    }

    public static BlockPos foldDelta(@Nullable Level level, Vec3i anchor, BlockPos target, BlockPos rawDelta) {
        if (level == null) {
            return rawDelta;
        }

        return delta(WorldLoopAttachments.wrappedFoldOfReader(level), asBlockPos(anchor), target, rawDelta);
    }

    public static BlockPos farEndDelta(@Nullable Level level, BlockPos anchor, BlockPos delta, BlockPos rawFarEnd) {
        if (level == null) {
            return rawFarEnd;
        }

        return farEndDelta(WorldLoopAttachments.foldOfReader(level), anchor, delta, rawFarEnd);
    }

    public static BlockPos worldSeat(@Nullable Level level, BlockPos stored) {
        if (level == null) {
            return stored;
        }

        Vec3 seated = ForeignFrames.seatInWorld(level, Vec3.atCenterOf(stored));
        return BlockPos.containing(seated);
    }

    public static BlockPos nearestCopy(@Nullable Level level, Vec3i anchor, BlockPos target) {
        return nearest(wrappedFoldOf(level, null), asBlockPos(anchor), target);
    }

    public static Vec3 nearestCopy(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return nearestCopy(wrappedFoldOf(level, null), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable ResourceKey<Level> dimension, Vec3 anchor, Vec3 target) {
        return nearestCopy(wrappedFoldOf(null, dimension), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable Level level, @Nullable ResourceKey<Level> dimension, Vec3 anchor,
            Vec3 target) {
        return nearestCopy(wrappedFoldOf(level, dimension), anchor, target);
    }

    private static Vec3 nearestCopy(@Nullable WorldFold fold, Vec3 anchor, Vec3 target) {
        return NearestCopy.toward(fold, anchor, target);
    }

    public static DeckTransformation nearestCopyTransformation(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return NearestCopy.transformationToward(wrappedFoldOf(level, null), anchor, target);
    }

    public static BlockPos foldPositionToBox(@Nullable Level level, BoundingBox box, Vec3i position) {
        BlockPos block = asBlockPos(position);
        if (level == null) {
            return block;
        }

        return foldPositionToBox(WorldLoopAttachments.wrappedFoldOfReader(level), box, block);
    }

    static BlockPos foldPositionToBox(@Nullable WorldFold fold, BoundingBox box, BlockPos position) {
        return nearest(fold, box.getCenter(), position);
    }

    public static Vec3 foldPointToBox(@Nullable Level level, AABB box, Vec3 point) {
        return NearestCopy.toward(WorldLoopAttachments.wrappedFoldOfReader(level), box.getCenter(), point);
    }

    public static Vec3 inFrameOf(WorldFold fold, Vec3 viewer, Vec3 anchor, Vec3 point) {
        return CreateFrameSeat.of(fold, viewer, anchor).apply(point);
    }

    public static BlockHitResult canonical(@Nullable ServerLevel level, BlockHitResult hit) {
        if (level == null) {
            return hit;
        }

        return canonical(WorldLoopAttachments.foldOf(level), hit);
    }

    static BlockHitResult canonical(WorldFold fold, BlockHitResult hit) {
        BlockPos raw = hit.getBlockPos();
        BlockPos wrapped = fold.fold(raw);
        if (wrapped.equals(raw)) {
            return hit;
        }

        Vec3 offsetInBlock = hit.getLocation().subtract(Vec3.atLowerCornerOf(raw));
        Vec3 location = Vec3.atLowerCornerOf(wrapped).add(offsetInBlock);
        return hit.getType() == HitResult.Type.MISS
                ? BlockHitResult.miss(location, hit.getDirection(), wrapped)
                : new BlockHitResult(location, hit.getDirection(), wrapped, hit.isInside());
    }

    public static BlockPos canonical(@Nullable ResourceKey<Level> dimension, BlockPos position) {
        return foldOf(null, dimension).fold(position);
    }

    public static BlockPos canonical(@Nullable ServerLevel level, BlockPos position) {
        return WorldLoopAttachments.foldOfReader(level).fold(position);
    }

    public static Vec3 canonical(@Nullable ServerLevel level, Vec3 position) {
        return WorldLoopAttachments.foldOfReader(level).fold(position);
    }

    public static BlockPos canonicalOnServer(@Nullable BlockGetter level, BlockPos position) {
        return level instanceof ServerLevel serverLevel ? canonical(serverLevel, position) : position;
    }

    public static Vec3 canonicalOnServer(@Nullable BlockGetter level, Vec3 position) {
        return level instanceof ServerLevel serverLevel ? canonical(serverLevel, position) : position;
    }

    public static BlockHitResult canonicalOnServer(@Nullable BlockGetter level, BlockHitResult hit) {
        return level instanceof ServerLevel serverLevel ? canonical(serverLevel, hit) : hit;
    }

    // Off the server thread the write is the viewer's frame, and canonicalising it jumps the carriage a world width.
    public static Vec3 canonicalOnServer(@Nullable ResourceKey<Level> dimension, Vec3 position) {
        MinecraftServer server = CurrentServer.get();
        if (server == null || !server.isSameThread()) {
            return position;
        }

        return foldOf(null, dimension).fold(position);
    }

    static BlockPos delta(@Nullable WorldFold fold, BlockPos anchor, BlockPos target,
            BlockPos rawDelta) {
        BlockPos nearest = nearest(fold, anchor, target);
        if (nearest.equals(target)) {
            return rawDelta;
        }

        return nearest.subtract(anchor);
    }

    static BlockPos farEndDelta(WorldFold fold, BlockPos anchor, BlockPos delta,
            BlockPos rawFarEnd) {
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

    public static BlockPos asBlockPos(Vec3i position) {
        return position instanceof BlockPos block ? block : new BlockPos(position);
    }

    private CreateSeamFold() {
    }
}
