package com.exoticworlds.engine.seam;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.accessors.ClientPositionHolder;
import com.exoticworlds.core.ForeignFrames;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.LogRateGate;
import com.exoticworlds.engine.LogRateGates;
import com.exoticworlds.engine.fold.SeamDelta;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ClientPosition {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Written on the server thread and read on the network thread, so the five values change together or not at all.
    private record Mirror(double x, double z, @Nullable ResourceKey<Level> space, @Nullable Level level,
            WorldFold transformer) {
    }

    public record BorderCenter(double x, double z) {
    }

    private volatile Mirror mirror = new Mirror(0.0, 0.0, null, null, WorldFolds.NOOP);

    private volatile @Nullable BlockPos heldSpawn;

    private volatile @Nullable BorderCenter heldBorderCenter;

    // One record because the two coordinates and their space are one fact: written on the server thread, read on the network thread.
    private volatile @Nullable ChunkPos heldCacheCenter;

    private final LogRateGate warnGate = new LogRateGate();

    private final LogRateGates translationWarnGates = new LogRateGates();

    public LogRateGates translationWarnGates() {
        return translationWarnGates;
    }

    public static ClientPosition of(ServerPlayer player) {
        return ((ClientPositionHolder) player.connection).toroidal$clientPosition();
    }

    public double x() {
        return seededMirror().x();
    }

    public double z() {
        return seededMirror().z();
    }

    private Mirror seededMirror() {
        Mirror currMirror = this.mirror;
        if (currMirror.space() == null) {
            throw new IllegalStateException("ClientPosition mirror read before the first rebase seeded it");
        }
        return currMirror;
    }

    public void set(Vec3 reported, MirrorWriter writer) {
        Mirror currMirror = this.mirror;
        boolean foreign = isForeign(currMirror.transformer(), reported);
        Vec3 world = foreign ? ForeignFrames.seatInWorld(currMirror.level(), reported) : reported;
        if (foreign && isForeign(currMirror.transformer(), world)) {
            return;
        }

        Vec3 seated = clientCopy(writer, foreign, currMirror, world);
        checkStep(writer, currMirror, seated);
        this.mirror = new Mirror(seated.x, seated.z, currMirror.space(), currMirror.level(), currMirror.transformer());
    }

    public boolean describes(ResourceKey<Level> dimension) {
        return dimension.equals(this.mirror.space());
    }

    public static void rebase(ServerPlayer player) {
        if (player.connection == null) {
            return;
        }

        WorldFold transformer = WorldLoopAttachments.transformerOf(player.level());
        Vec3 position = player.position();
        Vec3 world = isForeign(transformer, position) ? ForeignFrames.seatInWorld(player.level(), position) : position;
        Vec3 folded = transformer.fold(world);
        of(player).rebase(folded.x, folded.z, player.level().dimension(), player.level(), transformer);
    }

    public void rebase(double x, double z, ResourceKey<Level> dimension, @Nullable Level level,
            WorldFold transformer) {
        this.mirror = new Mirror(x, z, dimension, level, transformer);
        this.heldSpawn = null;
        this.heldBorderCenter = null;
        this.heldCacheCenter = null;
    }

    public @Nullable BlockPos heldSpawn() {
        return this.heldSpawn;
    }

    public void setHeldSpawn(BlockPos heldSpawn) {
        this.heldSpawn = heldSpawn;
    }

    public @Nullable BorderCenter heldBorderCenter() {
        return this.heldBorderCenter;
    }

    public void setHeldBorderCenter(BorderCenter heldBorderCenter) {
        this.heldBorderCenter = heldBorderCenter;
    }

    public @Nullable ChunkPos heldCacheCenter() {
        return this.heldCacheCenter;
    }

    public void setHeldCacheCenter(ChunkPos heldCacheCenter) {
        this.heldCacheCenter = heldCacheCenter;
    }

    public ChunkPos chunk() {
        Mirror currMirror = seededMirror();
        return new ChunkPos(
                SectionPos.blockToSectionCoord(currMirror.x()),
                SectionPos.blockToSectionCoord(currMirror.z()));
    }

    public Vec3 destinationOf(WorldFold fold, Vec3 position, Set<RelativeMovement> relatives) {
        Mirror currMirror = seededMirror();
        boolean relativeX = relatives.contains(RelativeMovement.X);
        boolean relativeZ = relatives.contains(RelativeMovement.Z);
        if (relativeX && relativeZ) {
            Vec3 step = SeamDelta.fold(fold, position.x, position.z);
            return new Vec3(currMirror.x() + step.x, position.y, currMirror.z() + step.z);
        }

        Vec3 reported = new Vec3(
                relativeX ? currMirror.x() + position.x : position.x,
                position.y,
                relativeZ ? currMirror.z() + position.z : position.z);
        return fold.nearestCopy(new Vec3(currMirror.x(), position.y, currMirror.z()), reported);
    }

    // The sub-level pose carries a rotation, so the world X of a foreign value depends on all three of its axes.
    private static boolean isForeign(WorldFold transformer, Vec3 position) {
        TranslationLattice lattice = transformer.blockLattice();
        return lattice.x().isForeign(position.x) || lattice.z().isForeign(position.z);
    }

    // destinationOf unwraps a server value already, but it bails on a foreign one, so a seated value arrives raw.
    private static Vec3 clientCopy(MirrorWriter writer, boolean seated, Mirror currMirror, Vec3 reported) {
        return writer.needsSeating() || seated
                ? currMirror.transformer().nearestCopy(new Vec3(currMirror.x(), reported.y, currMirror.z()), reported)
                : reported;
    }

    private void checkStep(MirrorWriter writer, Mirror from, Vec3 to) {
        Vec3 step = new Vec3(to.x - from.x(), 0.0, to.z - from.z());
        if (SeamDelta.fold(from.transformer(), step).equals(step) || !warnGate.tryPass()) {
            return;
        }

        LOGGER.warn("Half-world step invariant violated in {} by {}: mirror stepped from ({}, {}) to ({}, {}) without a rebase",
                spaceName(from.space()), writer.key(), from.x(), from.z(), to.x, to.z);
    }

    private static Object spaceName(@Nullable ResourceKey<Level> space) {
        return space == null ? "unseeded space" : space.location();
    }
}
