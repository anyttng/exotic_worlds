package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.accessors.ChunkResender;
import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.accessors.SeamDriveScheduler;
import com.toroidalworld.accessors.TrackedEntityRefresher;
import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.gen.FloatingCrumbs;
import com.toroidalworld.engine.gen.SeamDriveRequest;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.toroidalworld.engine.noise.TerrainCeiling;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.phys.Vec3;

@Mixin(ChunkMap.class)
public class ChunkMapMixin implements LevelHolder, ChunkResender, SeamDriveScheduler, TrackedEntityRefresher {
    @Unique
    private static final String toroidal$SECTION_POS_OF_ENTITY =
            "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/entity/EntityAccess;)Lnet/minecraft/core/SectionPos;";

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    private void updateChunkTracking(ServerPlayer player) {
        throw new AssertionError();
    }

    @Shadow
    private void applyChunkTrackingView(ServerPlayer player, ChunkTrackingView next) {
        throw new AssertionError();
    }

    @Shadow
    private void markChunkPendingToSend(ServerPlayer player, ChunkPos pos) {
        throw new AssertionError();
    }

    @Shadow
    private static void dropChunk(ServerPlayer player, ChunkPos pos) {
        throw new AssertionError();
    }

    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;

    @Shadow
    public @Nullable ChunkHolder getUpdatingChunkIfPresent(long key) {
        throw new AssertionError();
    }

    @Inject(method = "save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z", at = @At("RETURN"))
    private void toroidal$releaseTerrainMask(ChunkAccess chunk, CallbackInfoReturnable<Boolean> callback) {
        long key = chunk.getPos().pack();
        if (this.getUpdatingChunkIfPresent(key) == null && !this.pendingUnloads.containsKey(key)) {
            FloatingCrumbs.releaseMask(this.level, chunk.getPos());
        }
    }

    @Override
    public void toroidal$dropTrackedChunks(ServerPlayer player) {
        this.applyChunkTrackingView(player, ChunkTrackingView.EMPTY);
    }

    @Override
    public void toroidal$resendTrackedChunks(ServerPlayer player) {
        this.updateChunkTracking(player);
    }

    @Override
    public void toroidal$dropChunks(ServerPlayer player, List<ChunkPos> chunks) {
        for (ChunkPos chunkPos : chunks) {
            dropChunk(player, chunkPos);
        }
    }

    @Override
    public void toroidal$resendChunks(ServerPlayer player, List<ChunkPos> chunks) {
        for (ChunkPos chunkPos : chunks) {
            this.markChunkPendingToSend(player, chunkPos);
        }
    }

    @Override
    public void toroidal$refreshTrackedEntities(ServerPlayer player) {
        if (WorldLoopAttachments.wrappedTransformerOf(this.level) == null) {
            return;
        }

        ChunkMap chunkMap = (ChunkMap) (Object) this;
        for (ChunkMap.TrackedEntity tracked : chunkMap.entityMap.values()) {
            tracked.updatePlayer(player);
        }
    }

    @Inject(
            method = "setServerViewDistance",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap;updateChunkTracking(Lnet/minecraft/server/level/ServerPlayer;)V",
                    shift = At.Shift.AFTER))
    private void toroidal$refreshTrackingOnServerViewChange(int newViewDistance, CallbackInfo ci,
            @Local ServerPlayer player) {
        this.toroidal$refreshTrackedEntities(player);
    }

    @Shadow
    public DistanceManager getDistanceManager() {
        throw new AssertionError();
    }

    @WrapOperation(
            method = "applyStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StaticCache2D;get(II)Ljava/lang/Object;"))
    private Object toroidal$stepRunsOnItsOwnHolder(
            StaticCache2D<GenerationChunkHolder> cache,
            int chunkX,
            int chunkZ,
            Operation<Object> original,
            @Local(argsOnly = true) GenerationChunkHolder chunkHolder) {
        if (WorldLoopAttachments.wrappedTransformerOf(this.level) == null) {
            return original.call(cache, chunkX, chunkZ);
        }

        return chunkHolder;
    }

    @WrapOperation(
            method = "getChunkRangeFuture",
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_PACK))
    private long toroidal$rangeOverPhysicalChunks(int chunkX, int chunkZ, Operation<Long> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original.call(chunkX, chunkZ);
        }

        long folded = transformer.foldChunkKey(ChunkPos.pack(chunkX, chunkZ));
        return original.call(ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

    @Unique
    private final ConcurrentLinkedQueue<SeamDriveRequest> toroidal$driveRequests = new ConcurrentLinkedQueue<>();

    @Override
    public void toroidal$requestDrive(GenerationChunkHolder holder, ChunkStatus status) {
        if (WorldLoopAttachments.wrappedTransformerOf(this.level) == null) {
            return;
        }

        this.toroidal$driveRequests.add(new SeamDriveRequest(holder, status));
    }

    @ModifyVariable(method = "getLatestStatus", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldLatestStatusKey(long key) {
        return WorldLoopAttachments.transformerOf(this.level).foldChunkKey(key);
    }

    @Inject(method = "runGenerationTasks", at = @At("HEAD"))
    private void toroidal$driveWaitedOnHolders(CallbackInfo ci) {
        ChunkMap scheduler = (ChunkMap) (Object) this;
        SeamDriveRequest request;
        while ((request = this.toroidal$driveRequests.poll()) != null) {
            request.holder().scheduleChunkGenerationTask(request.status(), scheduler);
        }
    }

    @Override
    public ServerLevel toroidal$level() {
        return this.level;
    }

    @Shadow
    @Final
    private TicketStorage ticketStorage;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/RandomState;create("
                            + "Lnet/minecraft/core/HolderGetter;J"
                            + "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;)"
                            + "Lnet/minecraft/world/level/levelgen/RandomState;"))
    private RandomState toroidal$bindRouterBuild(
            HolderGetter<NormalNoise> noises,
            long seed,
            NoiseGeneratorSettings settings,
            Operation<RandomState> original,
            @Local(argsOnly = true) ChunkGenerator generator) {
        WorldFold fold = ShapedChunkGenerator.wrappedTransformerOf(generator);
        NoiseGeneratorSettings shaped = fold != null ? TerrainCeiling.withCeiling(settings) : settings;
        return GenerationTransformerContext.withRouterBuild(fold,
                () -> original.call(noises, seed, shaped));
    }

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/RandomState;create("
                            + "Lnet/minecraft/core/HolderGetter;JZ"
                            + "Lnet/minecraft/world/level/block/state/BlockState;I"
                            + "Lnet/minecraft/world/level/levelgen/NoiseRouter;)"
                            + "Lnet/minecraft/world/level/levelgen/RandomState;"))
    private RandomState toroidal$bindPlainRouterBuild(
            HolderGetter<NormalNoise> noises,
            long seed,
            boolean useLegacyRandom,
            BlockState defaultBlock,
            int seaLevel,
            NoiseRouter router,
            Operation<RandomState> original,
            @Local(argsOnly = true) ChunkGenerator generator) {
        return GenerationTransformerContext.withRouterBuild(ShapedChunkGenerator.wrappedTransformerOf(generator),
                () -> original.call(noises, seed, useLegacyRandom, defaultBlock, seaLevel, router));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindLevelToTickets(CallbackInfo ci) {
        ((LevelBindable) this.getDistanceManager()).toroidal$bindLevel(this.level);
        ((LevelBindable) this.ticketStorage).toroidal$bindLevel(this.level);
    }

    @WrapOperation(
            method = "updatePlayerStatus",
            at = @At(
                    value = "INVOKE",
                    target = toroidal$SECTION_POS_OF_ENTITY))
    private SectionPos toroidal$canonicalStatusSection(EntityAccess entity, Operation<SectionPos> original) {
        return toroidal$canonical(original.call(entity));
    }

    @WrapOperation(
            method = "updatePlayerPos",
            at = @At(
                    value = "INVOKE",
                    target = toroidal$SECTION_POS_OF_ENTITY))
    private SectionPos toroidal$canonicalPosSection(EntityAccess entity, Operation<SectionPos> original) {
        return toroidal$canonical(original.call(entity));
    }

    @WrapOperation(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = toroidal$SECTION_POS_OF_ENTITY))
    private SectionPos toroidal$canonicalMoveSection(EntityAccess entity, Operation<SectionPos> original) {
        return toroidal$canonical(original.call(entity));
    }

    @Unique
    private SectionPos toroidal$canonical(SectionPos section) {
        return WorldLoopAttachments.transformerOf(this.level).fold(section);
    }

    @WrapOperation(
            method = "updateChunkTracking",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos toroidal$canonicalViewCenter(ServerPlayer player, Operation<ChunkPos> original) {
        return WorldLoopAttachments.transformerOf(this.level).fold(original.call(player));
    }

    @WrapOperation(
            method = "updateChunkTracking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkTrackingView;of(Lnet/minecraft/world/level/ChunkPos;I)Lnet/minecraft/server/level/ChunkTrackingView;"))
    private ChunkTrackingView toroidal$trackWrapped(ChunkPos center, int viewDistance, Operation<ChunkTrackingView> original) {
        ChunkTrackingView view = original.call(center, viewDistance);
        ((TransformerHolder) (Object) view).toroidal$setTransformer(WorldLoopAttachments.transformerOf(this.level));
        return view;
    }

    @WrapMethod(method = "getPlayerViewDistance")
    private int toroidal$clampLoopedViewDistance(ServerPlayer player, Operation<Integer> original) {
        int vanilla = original.call(player);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        return transformer == null ? vanilla : transformer.limitViewDistance(vanilla);
    }

    @WrapOperation(
            method = "playerIsCloseEnoughForSpawning",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;euclideanDistanceSquared(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/phys/Vec3;)D"))
    private double toroidal$wrappedSpawnDistance(ChunkPos chunkPos, Vec3 pos, Operation<Double> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original.call(chunkPos, pos);
        }

        double chunkCenterX = SectionPos.sectionToBlockCoord(chunkPos.x(), 8);
        double chunkCenterZ = SectionPos.sectionToBlockCoord(chunkPos.z(), 8);
        return transformer.sqrDistance(pos.x, 0.0, pos.z, chunkCenterX, 0.0, chunkCenterZ);
    }

    @WrapOperation(
            method = "playerIsCloseEnoughTo",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double toroidal$wrappedPlayerDistance(Vec3 playerPos, Vec3 pos, Operation<Double> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original.call(playerPos, pos);
        }

        return Math.sqrt(transformer.sqrDistance(playerPos.x, playerPos.y, playerPos.z, pos.x, pos.y, pos.z));
    }
}
