package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.net.SeamContext;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.fold.RelativePosition;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.platform.Platforms;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        WorldFold transformer,
        ClientPosition clientPosition,
        IntFunction<RegistryFriendlyByteBuf> bufferFactory,
        ResourceKey<Level> dimension,
        int trackedViewDistance,
        int heldViewDistance,
        IntPredicate ownVehicle,
        IntFunction<@Nullable Vec3> entityPosition,
        IntFunction<TagPositions.@Nullable Subject> entity,
        Runnable rebase) implements SeamContext {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Two chunks: one of lighting border vanilla tracks past the view, one more where it forgets what fell out.
    private static final int VIEW_REACH_SLACK = 2;

    // Vanilla's own floor for a client's requested view distance, below which ChunkMap will not go.
    private static final int MIN_VIEW_DISTANCE = 2;

    private static final double REACH_MARGIN_BLOCKS =
            (double) CoordinateConstants.VIEW_DISTANCE_MARGIN * CoordinateConstants.CHUNK_WIDTH;

    public static TranslationContext of(ServerPlayer player, WorldFold transformer) {
        int trackedViewDistance = trackedViewDistanceOf(player, transformer);
        return new TranslationContext(
                transformer,
                ClientPosition.of(player),
                Platforms.get().packetBuffers(player),
                player.level().dimension(),
                trackedViewDistance,
                heldViewDistanceOf(player, trackedViewDistance),
                entityId -> isControlledVehicle(player, entityId),
                entityId -> positionOf(player, entityId),
                entityId -> subjectOf(player, entityId),
                () -> ClientPosition.rebase(player));
    }

    private static int trackedViewDistanceOf(ServerPlayer player, WorldFold transformer) {
        int serverViewDistance = player.level().getServer().getPlayerList().getViewDistance();
        return transformer.limitViewDistance(
                Mth.clamp(player.requestedViewDistance(), MIN_VIEW_DISTANCE, serverViewDistance));
    }

    private static int heldViewDistanceOf(ServerPlayer player, int trackedViewDistance) {
        return player.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view
                ? view.viewDistance()
                : trackedViewDistance;
    }

    private static boolean isControlledVehicle(ServerPlayer player, int entityId) {
        Entity vehicle = player.getControlledVehicle();
        return vehicle != null && vehicle.getId() == entityId;
    }

    private static @Nullable Vec3 positionOf(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity == null ? null : entity.position();
    }

    private static TagPositions.@Nullable Subject subjectOf(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity == null
                ? null
                : new TagPositions.Subject(entity.getClass(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    @Override
    public ToroidalShape shape() {
        return new ToroidalShapeView(transformer);
    }

    @Override
    public BlockPos toClient(BlockPos pos) {
        return transformer.reseat(pos, toClient(ChunkPos.containing(pos)));
    }

    @Override
    public ChunkPos toClient(ChunkPos chunkPos) {
        ChunkPos anchor = chunkAnchor();
        ChunkPos clientPos = transformer.nearestCopy(anchor, chunkPos);
        int viewReach = viewReach();
        if (chunkOutOfView(Direction.Axis.X, clientPos.x() - anchor.x(), viewReach)
                || chunkOutOfView(Direction.Axis.Z, clientPos.z() - anchor.z(), viewReach)) {
            warnChunkFarFromAnchor(chunkPos, clientPos, anchor, viewReach);
        }
        return clientPos;
    }

    private ChunkPos chunkAnchor() {
        ChunkPos heldCacheCenter = clientPosition.heldCacheCenter();
        return heldCacheCenter == null ? clientPosition.chunk() : heldCacheCenter;
    }

    public ChunkPos toClientCacheCenter(ChunkPos chunkPos) {
        ChunkPos clientPos = transformer.nearestCopy(clientPosition.chunk(), chunkPos);
        clientPosition.setHeldCacheCenter(clientPos);
        return clientPos;
    }

    public ChunkPos nearestCopy(ChunkPos chunkPos) {
        return transformer.nearestCopy(clientPosition.chunk(), chunkPos);
    }

    public BlockPos nearestCopy(BlockPos pos) {
        return transformer.reseat(pos, nearestCopy(ChunkPos.containing(pos)));
    }

    public List<ChunkPos> forgetCandidates(ChunkPos chunkPos) {
        ChunkPos anchor = clientPosition.chunk();
        ChunkPos nearest = transformer.nearestCopy(anchor, chunkPos);
        TranslationLattice lattice = transformer.chunkLattice();
        int ambiguityReach = copyAmbiguityReach();
        int[] xLaps = candidateLaps(lattice.x(), nearest.x() - anchor.x(), ambiguityReach);
        int[] zLaps = candidateLaps(lattice.z(), nearest.z() - anchor.z(), ambiguityReach);

        List<ChunkPos> candidates = new ArrayList<>(xLaps.length * zLaps.length);
        for (int xLap : xLaps) {
            for (int zLap : zLaps) {
                candidates.add(new ChunkPos(
                        nearest.x() + xLap * lattice.x().domainLength + zLap * lattice.skew(),
                        nearest.z() + zLap * lattice.z().domainLength));
            }
        }

        return candidates;
    }

    private static int[] candidateLaps(WrapDomain domain, int delta, int ambiguityReach) {
        if (Math.abs(delta) <= ambiguityReach || !domain.loops()) {
            return new int[] {0};
        }

        return new int[] {0, delta > 0 ? -1 : 1};
    }

    private int viewReach() {
        return heldViewDistance + VIEW_REACH_SLACK;
    }

    private int copyAmbiguityReach() {
        return transformer.maxViewDistance() + VIEW_REACH_SLACK;
    }

    public Vec3 toClientRelative(Vec3 position, boolean relativeX, boolean relativeZ, PacketReach reach) {
        Vec3 anchor = new Vec3(clientPosition.x(), position.y, clientPosition.z());
        Vec3 clientPos = RelativePosition.moved(position, relativeX, relativeZ, anchor,
                held -> transformer.nearestCopy(anchor, held));
        if (!relativeX) {
            guardReach(reach, Direction.Axis.X, position.x, clientPos.x, anchor.x);
        }
        if (!relativeZ) {
            guardReach(reach, Direction.Axis.Z, position.z, clientPos.z, anchor.z);
        }
        return clientPos;
    }

    public PacketReach trackedReach() {
        return PacketReach.tracked(trackedViewDistance);
    }

    private void guardReach(PacketReach reach, Direction.Axis axis, double serverValue, double clientValue,
            double anchor) {
        if (!withinReach(clientValue, anchor, reach)
                && carriesReach(rectangleAxis(transformer.blockLattice(), axis), reach)) {
            warnCoordFarFromAnchor(reach, axis, serverValue, clientValue, anchor);
        }
    }

    private boolean chunkOutOfView(Direction.Axis axis, int delta, int viewReach) {
        return Math.abs(delta) > viewReach && carriesView(rectangleAxis(transformer.chunkLattice(), axis), viewReach);
    }

    private static WrapDomain rectangleAxis(TranslationLattice lattice, Direction.Axis axis) {
        return axis == Direction.Axis.X ? lattice.x() : lattice.z();
    }

    static boolean withinReach(double clientValue, double anchor, PacketReach reach) {
        return Math.abs(clientValue - anchor) <= reach.blocks() + reach.slackBlocks();
    }

    static boolean carriesReach(WrapDomain blockDomain, PacketReach reach) {
        return blockDomain.fitsInHalf(reach.blocks() + reach.slackBlocks() + REACH_MARGIN_BLOCKS);
    }

    static boolean carriesView(WrapDomain chunkDomain, int viewReach) {
        return chunkDomain.fitsInHalf(viewReach + CoordinateConstants.VIEW_DISTANCE_MARGIN);
    }

    private enum Warning {
        CHUNK_FAR_FROM_ANCHOR,
        COORD_FAR_FROM_ANCHOR_X,
        COORD_FAR_FROM_ANCHOR_Z;

        static Warning coord(Direction.Axis axis) {
            return switch (axis) {
                case X -> COORD_FAR_FROM_ANCHOR_X;
                case Z -> COORD_FAR_FROM_ANCHOR_Z;
                case Y -> throw new IllegalArgumentException("A packet's reach is never guarded on the Y axis");
            };
        }
    }

    private void warnChunkFarFromAnchor(ChunkPos serverPos, ChunkPos clientPos, ChunkPos anchor, int viewReach) {
        if (!clientPosition.translationWarnGates().tryPass(Warning.CHUNK_FAR_FROM_ANCHOR)) {
            return;
        }

        LOGGER.warn("A chunk lands farther from the client anchor than the view reaches in {}:"
                        + " server {} translated to client {} around anchor {}, view reach {} chunks",
                dimension.identifier(), serverPos, clientPos, anchor, viewReach);
    }

    private void warnCoordFarFromAnchor(PacketReach reach, Direction.Axis axis,
            double serverValue, double clientValue, double anchor) {
        if (!clientPosition.translationWarnGates().tryPass(Warning.coord(axis))) {
            return;
        }

        LOGGER.warn("A {} packet's {} lands farther from the client anchor than it can reach in {}:"
                        + " server {} translated to client {} around anchor {}, reach {} blocks, slack {} blocks",
                reach.kind(), axis.getName(), dimension.identifier(), serverValue, clientValue, anchor,
                reach.blocks(), reach.slackBlocks());
    }

    @Override
    public Vec3 toClient(Vec3 position) {
        return toClient(position, trackedReach());
    }

    public Vec3 toClientMeasured(Vec3 position, String kind) {
        double blocks = MeasuredReach.blocks();
        return blocks == MeasuredReach.UNMEASURED
                ? nearestCopy(position)
                : toClient(position, PacketReach.measured(kind, blocks));
    }

    public Vec3 toClient(Vec3 position, PacketReach reach) {
        Vec3 clientPos = nearestCopy(position);
        guardReach(reach, Direction.Axis.X, position.x, clientPos.x, clientPosition.x());
        guardReach(reach, Direction.Axis.Z, position.z, clientPos.z, clientPosition.z());
        return clientPos;
    }

    public Vec3 nearestCopy(Vec3 position) {
        return transformer.nearestCopy(new Vec3(clientPosition.x(), position.y, clientPosition.z()), position);
    }

    public DeckTransformation nearestCopyTransformation(Vec3 position) {
        return transformer.nearestCopyTransformation(
                new Vec3(clientPosition.x(), position.y, clientPosition.z()), position);
    }

    @Override
    public BlockPos toServer(BlockPos pos) {
        return transformer.fold(pos);
    }

    public WorldFold.Folded<BlockPos> toServerOriented(BlockPos pos) {
        return transformer.foldOriented(pos);
    }

    @Override
    public Vec3 toServer(Vec3 position) {
        return transformer.fold(position);
    }
}
