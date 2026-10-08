package com.exoticworlds.engine.seam;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.accessors.ClientPositionHolder;
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
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ClientPosition {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Written on the server thread and read on the network thread, so the four values change together or not at all.
    private record Mirror(double x, double z, @Nullable ResourceKey<Level> space, WorldFold transformer) {
    }

    public record BorderCenter(double x, double z) {
    }

    private volatile Mirror mirror = new Mirror(0.0, 0.0, null, WorldFolds.NOOP);

    private volatile @Nullable BlockPos heldSpawn;

    private volatile @Nullable BorderCenter heldBorderCenter;

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

    public void set(double x, double z, MirrorWriter writer) {
        Mirror currMirror = this.mirror;
        Vec3 seated = clientCopy(writer, currMirror, new Vec3(x, 0.0, z));
        checkStep(writer, currMirror, seated);
        this.mirror = new Mirror(seated.x, seated.z, currMirror.space(), currMirror.transformer());
    }

    public boolean describes(ResourceKey<Level> dimension) {
        return dimension.equals(this.mirror.space());
    }

    public static void rebase(ServerPlayer player) {
        if (player.connection == null) {
            return;
        }

        WorldFold transformer = WorldLoopAttachments.transformerOf(player.level());
        Vec3 folded = transformer.fold(player.position());
        of(player).rebase(folded.x, folded.z, player.level().dimension(), transformer);
    }

    public void rebase(double x, double z, ResourceKey<Level> dimension, WorldFold transformer) {
        this.mirror = new Mirror(x, z, dimension, transformer);
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

    public Vec3 destinationOf(WorldFold fold, Vec3 position, Set<Relative> relatives) {
        Mirror currMirror = seededMirror();
        boolean relativeX = relatives.contains(Relative.X);
        boolean relativeZ = relatives.contains(Relative.Z);
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

    private static Vec3 clientCopy(MirrorWriter writer, Mirror currMirror, Vec3 reported) {
        return writer.needsSeating()
                ? currMirror.transformer().nearestCopy(new Vec3(currMirror.x(), 0.0, currMirror.z()), reported)
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
        return space == null ? "unseeded space" : space.identifier();
    }
}
