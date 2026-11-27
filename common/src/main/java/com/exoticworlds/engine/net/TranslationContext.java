package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.engine.fold.RelativePosition;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.HeldArea;
import com.exoticworlds.platform.Platforms;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record TranslationContext(
        WorldFold fold,
        ClientPosition clientPosition,
        IntFunction<RegistryFriendlyByteBuf> bufferFactory,
        ResourceKey<Level> dimension,
        int trackedViewDistanceChunks,
        int heldViewDistanceChunks,
        IntFunction<@Nullable Vec3> entityPosition,
        IntFunction<TagPositions.@Nullable Subject> entity,
        Runnable rebase,
        SeatPass seatPass) {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Two chunks: one of lighting border vanilla tracks past the view, one more where it forgets what fell out.
    private static final int VIEW_REACH_SLACK_CHUNKS = 2;

    // Vanilla's own floor for a client's requested view distance, below which ChunkMap will not go.
    private static final int MIN_VIEW_DISTANCE_CHUNKS = 2;

    private static final double REACH_MARGIN_BLOCKS =
            (double) CoordinateConstants.VIEW_DISTANCE_MARGIN_CHUNKS * CoordinateConstants.CHUNK_WIDTH_BLOCKS;

    public TranslationContext(WorldFold fold, ClientPosition clientPosition,
            IntFunction<RegistryFriendlyByteBuf> bufferFactory, ResourceKey<Level> dimension, int trackedViewDistanceChunks,
            int heldViewDistanceChunks, IntFunction<@Nullable Vec3> entityPosition,
            IntFunction<TagPositions.@Nullable Subject> entity, Runnable rebase) {
        this(fold, clientPosition, bufferFactory, dimension, trackedViewDistanceChunks, heldViewDistanceChunks, entityPosition,
                entity, rebase, new SeatPass());
    }

    public static TranslationContext followingCamera(ServerPlayer player, WorldFold fold) {
        ClientPosition.of(player).followCamera(player);
        return of(player, fold);
    }

    public static TranslationContext of(ServerPlayer player, WorldFold fold) {
        int trackedViewDistanceChunks = trackedViewDistanceChunksOf(player, fold);
        return new TranslationContext(
                fold,
                ClientPosition.of(player),
                Platforms.get().packetBuffers(player),
                player.level().dimension(),
                trackedViewDistanceChunks,
                getHeldViewDistanceChunks(player, trackedViewDistanceChunks),
                entityId -> getPosition(player, entityId),
                entityId -> getSubject(player, entityId),
                () -> ClientPosition.rebase(player));
    }

    private static int trackedViewDistanceChunksOf(ServerPlayer player, WorldFold fold) {
        int serverViewDistanceChunks = player.level().getServer().getPlayerList().getViewDistance();
        return fold.limitViewDistance(
                Mth.clamp(player.requestedViewDistance(), MIN_VIEW_DISTANCE_CHUNKS, serverViewDistanceChunks));
    }

    private static int getHeldViewDistanceChunks(ServerPlayer player, int trackedViewDistanceChunks) {
        return player.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view
                ? view.viewDistance()
                : trackedViewDistanceChunks;
    }

    private static @Nullable Vec3 getPosition(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity == null ? null : entity.position();
    }

    private static TagPositions.@Nullable Subject getSubject(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity == null
                ? null
                : new TagPositions.Subject(entity.getClass(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public BlockPos toClient(BlockPos pos) {
        return fold.reseat(pos, toClient(ChunkPos.containing(pos)));
    }

    public ChunkPos toClient(ChunkPos chunkPos) {
        ChunkPos anchor = getChunkAnchor();
        ChunkPos clientPos = fold.nearestCopy(anchor, chunkPos);
        int viewReachChunks = getViewReachChunks();
        if (isWithinView(clientPos, anchor, viewReachChunks)) {
            return seatInsideView(chunkPos, clientPos, viewReachChunks);
        }

        ChunkPos areaPos = heldAreaCopy(chunkPos);
        if (areaPos != null) {
            return areaPos;
        }

        ChunkPos cameraChunk = clientPosition.getCameraChunk();
        if (cameraChunk != null) {
            return fold.nearestCopy(cameraChunk, chunkPos);
        }

        guardView(chunkPos, clientPos, anchor, viewReachChunks);
        return clientPos;
    }

    // mc/1.21: Vista's zone re-check asks where a held zone's center would be seated now.
    public ChunkPos viewSeat(ChunkPos chunkPos) {
        ChunkPos anchor = getChunkAnchor();
        ChunkPos clientPos = fold.nearestCopy(anchor, chunkPos);
        ChunkPos cameraChunk = clientPosition.getCameraChunk();
        return cameraChunk == null || isWithinView(clientPos, anchor, getViewReachChunks())
                ? clientPos
                : fold.nearestCopy(cameraChunk, chunkPos);
    }

    private ChunkPos seatInsideView(ChunkPos chunkPos, ChunkPos clientPos, int viewReachChunks) {
        ChunkPos cameraPos = cameraCopyInsideView(chunkPos, clientPos, viewReachChunks);
        List<HeldArea> areas = clientPosition.getHeldAreas();
        if (cameraPos == null && areas.isEmpty()) {
            return clientPos;
        }

        List<ChunkPos> seats = new ArrayList<>();
        seats.add(clientPos);
        if (cameraPos != null) {
            seats.add(cameraPos);
        }

        for (HeldArea area : areas) {
            ChunkPos areaPos = area.copyOf(fold, chunkPos);
            if (areaPos != null) {
                addCandidate(seats, areaPos);
            }
        }

        seatPass.sawSeats(seats.size());
        int answered = seatPass.getAnswered();
        return answered < seats.size() ? seats.get(answered) : clientPos;
    }

    private @Nullable ChunkPos cameraCopyInsideView(ChunkPos chunkPos, ChunkPos clientPos, int viewReachChunks) {
        ChunkPos cameraChunk = clientPosition.getCameraChunk();
        if (cameraChunk == null) {
            return null;
        }

        ChunkPos cameraPos = fold.nearestCopy(cameraChunk, chunkPos);
        return !cameraPos.equals(clientPos) && isWithinView(cameraPos, cameraChunk, viewReachChunks) ? cameraPos : null;
    }

    private @Nullable ChunkPos heldAreaCopy(ChunkPos chunkPos) {
        for (HeldArea area : clientPosition.getHeldAreas()) {
            ChunkPos copy = area.copyOf(fold, chunkPos);
            if (copy != null) {
                return copy;
            }
        }

        return null;
    }

    private @Nullable Vec3 heldAreaCopy(Vec3 position) {
        if (clientPosition.getHeldAreas().isEmpty()) {
            return null;
        }

        ChunkPos chunkPos = new ChunkPos(
                SectionPos.blockToSectionCoord(position.x), SectionPos.blockToSectionCoord(position.z));
        ChunkPos anchor = getChunkAnchor();
        if (isWithinView(fold.nearestCopy(anchor, chunkPos), anchor, getViewReachChunks())) {
            return null;
        }

        ChunkPos areaPos = heldAreaCopy(chunkPos);
        return areaPos == null ? null : fold.deckTransformation(chunkPos, areaPos).apply(position);
    }

    private void guardView(ChunkPos chunkPos, ChunkPos clientPos, ChunkPos anchor, int viewReachChunks) {
        if (isChunkOutOfView(Direction.Axis.X, clientPos.x() - anchor.x(), viewReachChunks)
                || isChunkOutOfView(Direction.Axis.Z, clientPos.z() - anchor.z(), viewReachChunks)) {
            warnChunkFarFromAnchor(chunkPos, clientPos, anchor, viewReachChunks);
        }
    }

    private static boolean isWithinView(ChunkPos pos, ChunkPos center, int viewReachChunks) {
        return Math.abs(pos.x() - center.x()) <= viewReachChunks && Math.abs(pos.z() - center.z()) <= viewReachChunks;
    }

    private ChunkPos getChunkAnchor() {
        ChunkPos heldCacheCenter = clientPosition.getHeldCacheCenter();
        return heldCacheCenter == null ? clientPosition.getAnchorChunk() : heldCacheCenter;
    }

    public ChunkPos toClientCacheCenter(ChunkPos chunkPos) {
        ChunkPos clientPos = nearestCopy(chunkPos);
        clientPosition.setHeldCacheCenter(clientPos);
        return clientPos;
    }

    public ChunkPos nearestCopy(ChunkPos chunkPos) {
        return nearestCopy(fold, clientPosition, chunkPos);
    }

    public BlockPos nearestCopy(BlockPos pos) {
        return nearestCopy(fold, clientPosition, pos);
    }

    public static ChunkPos nearestCopy(WorldFold fold, ClientPosition clientPosition, ChunkPos chunkPos) {
        return fold.nearestCopy(clientPosition.getAnchorChunk(), chunkPos);
    }

    public static BlockPos nearestCopy(WorldFold fold, ClientPosition clientPosition, BlockPos pos) {
        return fold.reseat(pos, nearestCopy(fold, clientPosition, ChunkPos.containing(pos)));
    }

    public List<ChunkPos> forgetCandidates(ChunkPos chunkPos) {
        ChunkPos anchor = clientPosition.getAnchorChunk();
        ChunkPos nearest = fold.nearestCopy(anchor, chunkPos);
        TranslationLattice lattice = fold.getChunkLattice();
        int ambiguityReachLaps = copyAmbiguityReachLaps();
        int[] xLaps = candidateLaps(lattice.getX(), nearest.x() - anchor.x(), ambiguityReachLaps);
        int[] zLaps = candidateLaps(lattice.getZ(), nearest.z() - anchor.z(), ambiguityReachLaps);

        List<ChunkPos> candidates = new ArrayList<>(xLaps.length * zLaps.length);
        for (int xLap : xLaps) {
            for (int zLap : zLaps) {
                candidates.add(new ChunkPos(
                        nearest.x() + xLap * lattice.getX().domainLength + zLap * lattice.getSkew(),
                        nearest.z() + zLap * lattice.getZ().domainLength));
            }
        }

        if (clientPosition.getCameraChunk() != null) {
            addCandidate(candidates, fold.nearestCopy(getChunkAnchor(), chunkPos));
        }

        ChunkPos areaPos = heldAreaCopy(chunkPos);
        if (areaPos != null) {
            addCandidate(candidates, areaPos);
        }

        return candidates;
    }

    private static void addCandidate(List<ChunkPos> candidates, ChunkPos candidate) {
        if (!candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    private static int[] candidateLaps(WrapDomain domain, int delta, int ambiguityReachLaps) {
        if (Math.abs(delta) <= ambiguityReachLaps || !domain.isLooped()) {
            return new int[] {0};
        }

        return new int[] {0, delta > 0 ? -1 : 1};
    }

    private int getViewReachChunks() {
        return heldViewDistanceChunks + VIEW_REACH_SLACK_CHUNKS;
    }

    private int copyAmbiguityReachLaps() {
        return fold.getMaxViewDistanceChunks() + VIEW_REACH_SLACK_CHUNKS;
    }

    public Vec3 toClientRelative(Vec3 position, boolean isRelativeX, boolean isRelativeZ, PacketReach reach) {
        Vec3 anchor = clientPosition.anchor(position.y);
        Vec3 clientPos = RelativePosition.moved(position, isRelativeX, isRelativeZ, anchor,
                held -> fold.nearestCopy(anchor, held));
        if (!isRelativeX) {
            guardReach(reach, Direction.Axis.X, position.x, clientPos.x, anchor.x);
        }
        if (!isRelativeZ) {
            guardReach(reach, Direction.Axis.Z, position.z, clientPos.z, anchor.z);
        }
        return clientPos;
    }

    public PacketReach getTrackedReach() {
        return PacketReach.tracked(trackedViewDistanceChunks);
    }

    private void guardReach(PacketReach reach, Direction.Axis axis, double serverValue, double clientValue,
            double anchor) {
        if (!isWithinReach(clientValue, anchor, reach)
                && hasReach(fold.getBlockLattice().getAxis(axis), reach)) {
            warnCoordFarFromAnchor(reach, axis, serverValue, clientValue, anchor);
        }
    }

    private boolean isChunkOutOfView(Direction.Axis axis, int delta, int viewReachChunks) {
        return Math.abs(delta) > viewReachChunks && hasView(fold.getChunkLattice().getAxis(axis), viewReachChunks);
    }

    static boolean isWithinReach(double clientValue, double anchor, PacketReach reach) {
        return Math.abs(clientValue - anchor) <= reach.blocks() + reach.slackBlocks();
    }

    static boolean hasReach(WrapDomain blockDomain, PacketReach reach) {
        return blockDomain.canFitInHalf(reach.blocks() + reach.slackBlocks() + REACH_MARGIN_BLOCKS);
    }

    static boolean hasView(WrapDomain chunkDomain, int viewReachChunks) {
        return chunkDomain.canFitInHalf(viewReachChunks + CoordinateConstants.VIEW_DISTANCE_MARGIN_CHUNKS);
    }

    private enum Warning {
        CHUNK_FAR_FROM_ANCHOR,
        COORD_FAR_FROM_ANCHOR_X,
        COORD_FAR_FROM_ANCHOR_Z;

        static Warning getCoord(Direction.Axis axis) {
            return switch (axis) {
                case X -> COORD_FAR_FROM_ANCHOR_X;
                case Z -> COORD_FAR_FROM_ANCHOR_Z;
                case Y -> throw new IllegalArgumentException("A packet's reach is never guarded on the Y axis");
            };
        }
    }

    private void warnChunkFarFromAnchor(ChunkPos serverPos, ChunkPos clientPos, ChunkPos anchor, int viewReachChunks) {
        if (!clientPosition.getTranslationWarnGates().tryPass(Warning.CHUNK_FAR_FROM_ANCHOR)) {
            return;
        }

        LOGGER.warn("A chunk lands farther from the client anchor than the view reaches in {}:"
                        + " server {} translated to client {} around anchor {}, view reach {} chunks",
                dimension.identifier(), serverPos, clientPos, anchor, viewReachChunks);
    }

    private void warnCoordFarFromAnchor(PacketReach reach, Direction.Axis axis,
            double serverValue, double clientValue, double anchor) {
        if (!clientPosition.getTranslationWarnGates().tryPass(Warning.getCoord(axis))) {
            return;
        }

        LOGGER.warn("A {} packet's {} lands farther from the client anchor than it can reach in {}:"
                        + " server {} translated to client {} around anchor {}, reach {} blocks, slack {} blocks",
                reach.kind(), axis.getName(), dimension.identifier(), serverValue, clientValue, anchor,
                reach.blocks(), reach.slackBlocks());
    }

    public Vec3 toClient(Vec3 position) {
        return toClient(position, getTrackedReach());
    }

    public Vec3 toClientMeasured(Vec3 position, String kind) {
        double blocks = MeasuredReach.getBlocks();
        return blocks == MeasuredReach.UNMEASURED
                ? nearestCopy(position)
                : toClient(position, PacketReach.measured(kind, blocks));
    }

    public Vec3 toClient(Vec3 position, PacketReach reach) {
        Vec3 areaPos = heldAreaCopy(position);
        if (areaPos != null) {
            return areaPos;
        }

        Vec3 anchor = clientPosition.anchor(position.y);
        Vec3 clientPos = fold.nearestCopy(anchor, position);
        guardReach(reach, Direction.Axis.X, position.x, clientPos.x, anchor.x);
        guardReach(reach, Direction.Axis.Z, position.z, clientPos.z, anchor.z);
        return clientPos;
    }

    public Vec3 nearestCopy(Vec3 position) {
        return fold.nearestCopy(clientPosition.anchor(position.y), position);
    }

    public DeckTransformation nearestCopyTransformation(Vec3 position) {
        return fold.nearestCopyTransformation(clientPosition.anchor(position.y), position);
    }

    public BlockPos toServer(BlockPos pos) {
        return fold.fold(pos);
    }

    public WorldFold.Folded<BlockPos> toServerOriented(BlockPos pos) {
        return fold.foldOriented(pos);
    }

    public Vec3 toServer(Vec3 position) {
        return fold.fold(position);
    }

    public ChunkPos toServer(ChunkPos chunkPos) {
        return fold.fold(chunkPos);
    }
}
