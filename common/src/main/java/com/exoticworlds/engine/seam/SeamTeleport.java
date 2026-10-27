package com.exoticworlds.engine.seam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ChunkResender;
import com.exoticworlds.accessors.TrackedEntityRefresher;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.RelativePosition;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class SeamTeleport {
    public record Dropped(boolean wholeView, List<ChunkPos> flipped) {
    }

    public static PositionMoveRotation destination(ServerPlayer player, PositionMoveRotation destination,
            Set<Relative> relatives) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(player.level());
        return fold == null ? destination : destination(fold, player.position(), destination, relatives);
    }

    static PositionMoveRotation destination(WorldFold fold, Vec3 anchor, PositionMoveRotation destination,
            Set<Relative> relatives) {
        Vec3 position = destination.position();
        Vec3 wrapped = RelativePosition.moved(position, relatives.contains(Relative.X), relatives.contains(Relative.Z),
                anchor, fold::fold);
        if (wrapped == position) {
            return destination;
        }

        return new PositionMoveRotation(wrapped, destination.deltaMovement(), destination.yRot(), destination.xRot());
    }

    public static @Nullable Dropped dropBefore(ServerPlayer player, PositionMoveRotation destination,
            Set<Relative> relatives) {
        ChunkResender resender = chunkResender(player);
        if (resender == null) {
            return null;
        }

        ClientPosition mirror = ClientPosition.of(player);
        if (!mirror.describes(player.level().dimension())) {
            resender.exotic_worlds$dropTrackedChunks(player);
            return new Dropped(true, List.of());
        }

        WorldFold fold = WorldLoopAttachments.foldOf(player.level());
        Vec3 clientDestination = mirror.destinationOf(fold, destination.position(), relatives);
        ChunkPos toAnchor = new ChunkPos(
                SectionPos.blockToSectionCoord(clientDestination.x),
                SectionPos.blockToSectionCoord(clientDestination.z));
        List<ChunkPos> flipped = flipped(fold, mirror.seatChunk(), toAnchor, player.getChunkTrackingView());
        if (!flipped.isEmpty()) {
            resender.exotic_worlds$dropChunks(player, flipped);
        }
        return new Dropped(false, flipped);
    }

    public static void resendAfter(ServerPlayer player, @Nullable Dropped dropped) {
        ChunkResender resender = chunkResender(player);
        if (resender == null) {
            return;
        }

        if (dropped != null && dropped.wholeView()) {
            resender.exotic_worlds$resendTrackedChunks(player);
        } else if (dropped != null && !dropped.flipped().isEmpty()) {
            resender.exotic_worlds$resendChunks(player, dropped.flipped());
        }

        ((TrackedEntityRefresher) (Object) player.level().getChunkSource().chunkMap)
                .exotic_worlds$refreshTrackedEntities(player);
    }

    static List<ChunkPos> flipped(WorldFold fold, ChunkPos fromAnchor, ChunkPos toAnchor, ChunkTrackingView view) {
        List<ChunkPos> flipped = new ArrayList<>();
        view.forEach(viewPos -> {
            ChunkPos physical = fold.fold(viewPos);
            if (!fold.nearestCopy(fromAnchor, physical).equals(fold.nearestCopy(toAnchor, physical))) {
                flipped.add(viewPos);
            }
        });
        return flipped;
    }

    private static @Nullable ChunkResender chunkResender(ServerPlayer player) {
        if (WorldLoopAttachments.wrappedFoldOf(player.level()) == null) {
            return null;
        }

        return (ChunkResender) (Object) player.level().getChunkSource().chunkMap;
    }

    private SeamTeleport() {
    }
}
