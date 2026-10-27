package com.exoticworlds.engine.seam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ChunkResender;
import com.exoticworlds.accessors.TrackedEntityRefresher;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class SeamTeleport {
    public record Dropped(boolean wholeView, List<ChunkPos> flipped) {
    }

    public static Vec3 destination(ServerPlayer player, double x, double y, double z) {
        return destination(WorldLoopAttachments.foldOf(player.level()), new Vec3(x, y, z));
    }

    static Vec3 destination(WorldFold fold, Vec3 destination) {
        return fold.fold(destination);
    }

    public static @Nullable Dropped dropBefore(ServerPlayer player, double destinationX, double destinationZ) {
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
        Vec3 clientDestination = mirror.destinationOf(fold, new Vec3(destinationX, 0.0, destinationZ), Set.of());
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

        ((TrackedEntityRefresher) (Object) player.serverLevel().getChunkSource().chunkMap)
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

        return (ChunkResender) (Object) player.serverLevel().getChunkSource().chunkMap;
    }

    private SeamTeleport() {
    }
}
