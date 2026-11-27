package com.exoticworlds.engine.seam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.accessors.ClientPositionHolder;
import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.engine.LogRateGate;
import com.exoticworlds.engine.LogRateGates;
import com.exoticworlds.engine.fold.SeamDelta;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ClientPosition {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double CHUNK_CENTER_BLOCKS = CoordinateConstants.CHUNK_WIDTH_BLOCKS / 2.0;

    // Written on the server thread and read on the network thread, so the five values change together or not at all.
    private record Mirror(double x, double z, @Nullable ResourceKey<Level> space, WorldFold fold,
            DeckTransformation chart) {
        Vec3 point() {
            return new Vec3(this.x, 0.0, this.z);
        }
    }

    private record CameraCopy(double x, double z, boolean isLeft) {
    }

    public record BorderCenter(double x, double z) {
    }

    private volatile Mirror mirror = new Mirror(0.0, 0.0, null, WorldFolds.NOOP, DeckTransformation.IDENTITY);

    private volatile @Nullable CameraCopy cameraCopy;

    private volatile @Nullable BlockPos heldSpawn;

    private volatile @Nullable BorderCenter heldBorderCenter;

    private volatile @Nullable ChunkPos heldCacheCenter;

    private volatile List<HeldArea> heldAreas = List.of();

    private final LogRateGate warnGate = new LogRateGate();

    private final LogRateGates translationWarnGates = new LogRateGates();

    public LogRateGates getTranslationWarnGates() {
        return translationWarnGates;
    }

    public static ClientPosition of(ServerPlayer player) {
        return ((ClientPositionHolder) player.connection).exotic_worlds$getClientPosition();
    }

    public double getX() {
        return getSeededMirror().x();
    }

    public double getZ() {
        return getSeededMirror().z();
    }

    private Mirror getSeededMirror() {
        Mirror currMirror = this.mirror;
        if (currMirror.space() == null) {
            throw new IllegalStateException("ClientPosition mirror read before the first rebase seeded it");
        }
        return currMirror;
    }

    public void set(double x, double z, MirrorWriter writer) {
        Mirror currMirror = this.mirror;
        Vec3 seated = clientCopy(writer, currMirror, new Vec3(x, 0.0, z));
        DeckTransformation chart = chartAfterStep(writer, currMirror, seated);
        if (currMirror.fold().isPreservingLocalIndices()) {
            checkStep(writer, stepOrigin(writer, currMirror), seated);
        }
        this.mirror = new Mirror(seated.x, seated.z, currMirror.space(), currMirror.fold(), chart);
        if (writer == MirrorWriter.POSITION_PACKET) {
            this.cameraCopy = null;
        }
    }

    private Mirror stepOrigin(MirrorWriter writer, Mirror currMirror) {
        CameraCopy copy = this.cameraCopy;
        return writer == MirrorWriter.POSITION_PACKET && copy != null
                ? new Mirror(copy.x(), copy.z(), currMirror.space(), currMirror.fold(), currMirror.chart())
                : currMirror;
    }

    public void followCamera(ServerPlayer player) {
        Entity camera = player.getCamera();
        followCamera(camera == player ? null : camera.position(), camera.level().dimension());
    }

    public void followCamera(@Nullable Vec3 camera, ResourceKey<Level> dimension) {
        CameraCopy copy = this.cameraCopy;
        if (camera == null) {
            if (copy != null && !copy.isLeft()) {
                this.cameraCopy = new CameraCopy(copy.x(), copy.z(), true);
            }
            return;
        }

        Mirror currMirror = this.mirror;
        if (!dimension.equals(currMirror.space())) {
            return;
        }

        Vec3 seated = nearestCopy(currMirror, currMirror.fold(), anchor(currMirror, copy, 0.0),
                new Vec3(camera.x, 0.0, camera.z));
        this.cameraCopy = new CameraCopy(seated.x, seated.z, false);
    }

    public void dropLeftCamera() {
        CameraCopy copy = this.cameraCopy;
        if (copy != null && copy.isLeft()) {
            this.cameraCopy = null;
        }
    }

    public Vec3 anchor(double y) {
        return anchor(getSeededMirror(), this.cameraCopy, y);
    }

    public ChunkPos getAnchorChunk() {
        return chunkOf(anchor(0.0));
    }

    public @Nullable ChunkPos getCameraChunk() {
        CameraCopy copy = this.cameraCopy;
        return copy == null || copy.isLeft() ? null : chunkOf(new Vec3(copy.x(), 0.0, copy.z()));
    }

    public ChunkPos getSeatChunk() {
        return chunkOf(seat(getSeededMirror(), 0.0));
    }

    private static Vec3 anchor(Mirror currMirror, @Nullable CameraCopy copy, double y) {
        return copy == null || copy.isLeft()
                ? new Vec3(currMirror.x(), y, currMirror.z())
                : new Vec3(copy.x(), y, copy.z());
    }

    private Vec3 seat(Mirror currMirror, double y) {
        CameraCopy copy = this.cameraCopy;
        return copy == null ? new Vec3(currMirror.x(), y, currMirror.z()) : new Vec3(copy.x(), y, copy.z());
    }

    public boolean isForDimension(ResourceKey<Level> dimension) {
        return dimension.equals(this.mirror.space());
    }

    public static void rebase(ServerPlayer player) {
        if (player.connection == null) {
            return;
        }

        WorldFold fold = WorldLoopAttachments.getFold(player.level());
        Vec3 folded = fold.fold(player.position());
        of(player).rebase(folded.x, folded.z, player.level().dimension(), fold);
    }

    public void rebase(double x, double z, ResourceKey<Level> dimension, WorldFold fold) {
        this.mirror = new Mirror(x, z, dimension, fold, DeckTransformation.IDENTITY);
        this.cameraCopy = null;
        this.heldSpawn = null;
        this.heldBorderCenter = null;
        this.heldCacheCenter = null;
        this.heldAreas = List.of();
    }

    public @Nullable BlockPos getHeldSpawn() {
        return this.heldSpawn;
    }

    public void setHeldSpawn(BlockPos heldSpawn) {
        this.heldSpawn = heldSpawn;
    }

    public @Nullable BorderCenter getHeldBorderCenter() {
        return this.heldBorderCenter;
    }

    public void setHeldBorderCenter(BorderCenter heldBorderCenter) {
        this.heldBorderCenter = heldBorderCenter;
    }

    public @Nullable ChunkPos getHeldCacheCenter() {
        return this.heldCacheCenter;
    }

    public void setHeldCacheCenter(ChunkPos heldCacheCenter) {
        this.heldCacheCenter = heldCacheCenter;
    }

    public List<HeldArea> getHeldAreas() {
        return this.heldAreas;
    }

    // mc/1.21: Vista's zone payload rewriter holds each zone at the seat it sends the zone's center to.
    public void setHeldAreas(List<HeldArea> heldAreas) {
        this.heldAreas = List.copyOf(heldAreas);
    }

    public ChunkPos getChunk() {
        Mirror currMirror = getSeededMirror();
        return chunkOf(new Vec3(currMirror.x(), 0.0, currMirror.z()));
    }

    private static ChunkPos chunkOf(Vec3 position) {
        return new ChunkPos(SectionPos.blockToSectionCoord(position.x), SectionPos.blockToSectionCoord(position.z));
    }

    private static Vec3 centerOf(ChunkPos chunkPos) {
        return new Vec3(chunkPos.getMinBlockX() + CHUNK_CENTER_BLOCKS, 0.0, chunkPos.getMinBlockZ() + CHUNK_CENTER_BLOCKS);
    }

    public FoldOrientation orientation(WorldFold fold) {
        Mirror currMirror = getSeededMirror();
        return orientationAt(currMirror, fold, currMirror.point());
    }

    public FoldOrientation orientationAt(WorldFold fold, Vec3 clientPoint) {
        return orientationAt(this.mirror, fold, clientPoint);
    }

    private static FoldOrientation orientationAt(Mirror currMirror, WorldFold fold, Vec3 clientPoint) {
        return fold.isPreservingLocalIndices()
                ? FoldOrientation.IDENTITY
                : chartAt(currMirror, fold, clientPoint).getOrientation().inverse();
    }

    public Vec3 toServerFrame(WorldFold fold, Vec3 clientPoint) {
        return chartAt(this.mirror, fold, clientPoint).apply(clientPoint);
    }

    public Vec3 nearestCopy(WorldFold fold, Vec3 clientRef, Vec3 target) {
        return nearestCopy(this.mirror, fold, clientRef, target);
    }

    private static Vec3 nearestCopy(Mirror currMirror, WorldFold fold, Vec3 clientRef, Vec3 target) {
        DeckTransformation chart = chartAt(currMirror, fold, clientRef);
        return chart.inverse().apply(fold.nearestCopy(chart.apply(clientRef), target));
    }

    public BlockPos nearestCopy(WorldFold fold, BlockPos clientRef, BlockPos target) {
        DeckTransformation chart = chartAt(this.mirror, fold, Vec3.atCenterOf(clientRef));
        return chart.inverse().apply(fold.nearestCopy(chart.apply(clientRef), target));
    }

    public ChunkPos nearestCopy(WorldFold fold, ChunkPos clientRef, ChunkPos target) {
        DeckTransformation chart = chartAt(this.mirror, fold, centerOf(clientRef));
        return chart.inverse().apply(fold.nearestCopy(chart.apply(clientRef), target));
    }

    public DeckTransformation nearestCopyTransformation(WorldFold fold, Vec3 clientRef, Vec3 target) {
        DeckTransformation chart = chartAt(this.mirror, fold, clientRef);
        return fold.nearestCopyTransformation(chart.apply(clientRef), target).then(chart.inverse());
    }

    public DeckTransformation nearestCopyTransformation(WorldFold fold, ChunkPos clientRef, ChunkPos target) {
        DeckTransformation chart = chartAt(this.mirror, fold, centerOf(clientRef));
        return fold.nearestCopyTransformation(chart.apply(clientRef), target).then(chart.inverse());
    }

    public List<ChunkPos> copiesInReach(WorldFold fold, ChunkPos clientRef, ChunkPos target, int reachChunks) {
        DeckTransformation chart = chartAt(this.mirror, fold, centerOf(clientRef));
        List<ChunkPos> serverCopies = fold.copiesInReach(chart.apply(clientRef), target, reachChunks);
        if (chart.isIdentity()) {
            return serverCopies;
        }

        DeckTransformation back = chart.inverse();
        List<ChunkPos> copies = new ArrayList<>(serverCopies.size());
        for (ChunkPos serverCopy : serverCopies) {
            copies.add(back.apply(serverCopy));
        }
        return copies;
    }

    private static DeckTransformation chartAt(Mirror currMirror, WorldFold fold, Vec3 clientPoint) {
        if (fold.isPreservingLocalIndices()) {
            return DeckTransformation.IDENTITY;
        }

        if (currMirror.fold() != fold) {
            return fold.foldTransformation(clientPoint);
        }

        DeckTransformation chart = currMirror.chart();
        Vec3 to = chart.apply(clientPoint);
        DeckTransformation along = fold.foldTransformationAlong(chart.apply(currMirror.point()), to);
        return chart.then(along == null ? fold.foldTransformation(to) : along);
    }

    public static Vec3 foldStep(WorldFold fold, Vec3 delta) {
        return fold.isPreservingLocalIndices() ? SeamDelta.fold(fold, delta) : delta;
    }

    public Vec3 destinationOf(WorldFold fold, Vec3 position, Set<Relative> relatives) {
        Mirror currMirror = getSeededMirror();
        boolean isRelativeX = relatives.contains(Relative.X);
        boolean isRelativeZ = relatives.contains(Relative.Z);
        if (isRelativeX && isRelativeZ) {
            Vec3 step = orientation(fold).applyToDelta(foldStep(fold, new Vec3(position.x, 0.0, position.z)));
            return new Vec3(currMirror.x() + step.x, position.y, currMirror.z() + step.z);
        }

        Vec3 reported = new Vec3(
                isRelativeX ? currMirror.x() + position.x : position.x,
                position.y,
                isRelativeZ ? currMirror.z() + position.z : position.z);
        Vec3 seat = isRelativeX || isRelativeZ
                ? new Vec3(currMirror.x(), position.y, currMirror.z())
                : seat(currMirror, position.y);
        return nearestCopy(currMirror, fold, seat, reported);
    }

    private static Vec3 clientCopy(MirrorWriter writer, Mirror currMirror, Vec3 reported) {
        if (!writer.isSeatingNeeded()) {
            return reported;
        }

        WorldFold fold = currMirror.fold();
        Vec3 serverPoint = chartAt(currMirror, fold, reported).apply(reported);
        return nearestCopy(currMirror, fold, currMirror.point(), serverPoint);
    }

    private DeckTransformation chartAfterStep(MirrorWriter writer, Mirror currMirror, Vec3 seated) {
        WorldFold fold = currMirror.fold();
        DeckTransformation chart = currMirror.chart();
        if (fold.isPreservingLocalIndices()) {
            return chart;
        }

        Vec3 from = chart.apply(currMirror.point());
        Vec3 to = chart.apply(seated);
        DeckTransformation along = fold.foldTransformationAlong(from, to);
        if (along != null) {
            return chart.then(along);
        }

        warnNoStraightStep(writer, currMirror, seated);
        return chart.then(fold.foldTransformation(to));
    }

    private void checkStep(MirrorWriter writer, Mirror from, Vec3 to) {
        Vec3 step = new Vec3(to.x - from.x(), 0.0, to.z - from.z());
        if (SeamDelta.fold(from.fold(), step).equals(step) || !warnGate.tryPass()) {
            return;
        }

        LOGGER.warn("Half-world step invariant violated in {} by {}: mirror stepped from ({}, {}) to ({}, {}) without a rebase",
                spaceName(from.space()), writer.getKey(), from.x(), from.z(), to.x, to.z);
    }

    private void warnNoStraightStep(MirrorWriter writer, Mirror from, Vec3 to) {
        if (!warnGate.tryPass()) {
            return;
        }

        LOGGER.warn("No straight step carries the mirror's chart in {} by {}: from ({}, {}) to ({}, {}),"
                        + " the chart falls back to folding the point",
                spaceName(from.space()), writer.getKey(), from.x(), from.z(), to.x, to.z);
    }

    private static Object spaceName(@Nullable ResourceKey<Level> space) {
        return space == null ? "unseeded space" : space.identifier();
    }
}
