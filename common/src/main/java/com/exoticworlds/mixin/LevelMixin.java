package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.ActiveFeaturesHolder;
import com.exoticworlds.accessors.FeatureStateHolder;
import com.exoticworlds.accessors.PeriodicityMark;
import com.exoticworlds.accessors.FoldCache;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.fold.BlockEntitySeat;
import com.exoticworlds.engine.fold.ChunkFrame;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.fold.SeamEntitySearch;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.check.SeamVerdict;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

@Mixin(Level.class)
public class LevelMixin implements FoldCache, ActiveFeaturesHolder, FeatureStateHolder, PeriodicityMark {
    @Unique
    private static final String exotic_worlds$GET_ENTITIES =
            "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;";

    @Unique
    private static final String GET_CHUNK =
            "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;";

    @Unique
    private static final String SOURCE_GET_CHUNK = "Lnet/minecraft/world/level/chunk/ChunkSource;"
            + "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;";

    @Unique
    private static final String SET_BLOCK =
            "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z";

    @Unique
    private WorldFold exotic_worlds$fold;

    @Unique
    private @Nullable List<GenerationFeature> exotic_worlds$activeFeatures;

    @Unique
    private final Map<String, Object> exotic_worlds$featureStates = new ConcurrentHashMap<>();

    @Unique
    private final AtomicReference<SeamVerdict> exotic_worlds$seamVerdict = new AtomicReference<>();

    @Unique
    private final AtomicReference<SeamVerdict> exotic_worlds$fullSeamVerdict = new AtomicReference<>();

    // Must stay above the Z handler: Mixin runs them in declaration order, and X folds by the unfolded Z.
    @ModifyArg(method = GET_CHUNK, at = @At(value = "INVOKE", target = SOURCE_GET_CHUNK), index = 0)
    private int exotic_worlds$chunkXThroughSeam(int chunkX, int chunkZ, ChunkStatus status, boolean loadOrGenerate) {
        WorldFold fold = exotic_worlds$getFold();
        return fold.isWrapped() ? ChunkPos.getX(fold.foldChunkKey(ChunkPos.pack(chunkX, chunkZ))) : chunkX;
    }

    @ModifyArg(method = GET_CHUNK, at = @At(value = "INVOKE", target = SOURCE_GET_CHUNK), index = 1)
    private int exotic_worlds$chunkZThroughSeam(int chunkX, int chunkZ, ChunkStatus status, boolean loadOrGenerate) {
        WorldFold fold = exotic_worlds$getFold();
        return fold.isWrapped() ? ChunkPos.getZ(fold.foldChunkKey(ChunkPos.pack(chunkX, chunkZ))) : chunkZ;
    }

    @ModifyVariable(method = "getBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapBlockEntityPos(BlockPos pos) {
        return exotic_worlds$wrap(pos);
    }

    @ModifyVariable(method = "removeBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapRemovedBlockEntityPos(BlockPos pos) {
        return exotic_worlds$wrap(pos);
    }

    @WrapOperation(
            method = "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I",
            at = @At(value = "INVOKE", target = InjectionTargets.LEVEL_CHUNK_GET_HEIGHT))
    private int exotic_worlds$heightInChunkFrame(LevelChunk chunk, Heightmap.Types type, int localX, int localZ,
            Operation<Integer> original, @Local(argsOnly = true, ordinal = 0) int x,
            @Local(argsOnly = true, ordinal = 1) int z) {
        long seated = ChunkFrame.seatColumn(exotic_worlds$getFold(), chunk.getPos(), x, z);
        return original.call(chunk, type,
                SectionPos.sectionRelative(BlockPos.getX(seated)), SectionPos.sectionRelative(BlockPos.getZ(seated)));
    }

    // Must stay above the position handler: Mixin runs them in declaration order, and the turn reads the unfolded position.
    @ModifyVariable(method = SET_BLOCK, at = @At("HEAD"), argsOnly = true)
    private BlockState exotic_worlds$turnPlacedState(BlockState state, @Local(argsOnly = true) BlockPos pos) {
        return ChunkFrame.toChunk(exotic_worlds$getFold(), pos, state);
    }

    @ModifyVariable(method = SET_BLOCK, at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapPlacedBlockPos(BlockPos pos) {
        return exotic_worlds$wrap(pos);
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void exotic_worlds$wrapBlockEntityIdentity(BlockEntity blockEntity, CallbackInfo ci) {
        BlockEntitySeat.intoBounds(exotic_worlds$getFold(), blockEntity);
    }

    @WrapOperation(
            method = exotic_worlds$GET_ENTITIES,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/LevelEntityGetter;get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V"))
    private void exotic_worlds$entitiesThroughSeam(LevelEntityGetter<Entity> entities, AABB box, Consumer<Entity> output,
            Operation<Void> original) {
        SeamEntitySearch.forEach(exotic_worlds$getFold(), box, output,
                (piece, sink) -> original.call(entities, piece, sink));
    }

    @WrapOperation(
            method = exotic_worlds$GET_ENTITIES,
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean exotic_worlds$partBoxTowardQuery(AABB query, AABB part, Operation<Boolean> original) {
        return original.call(query, FoldedBoxQuery.toward(exotic_worlds$getFold(), query.getCenter(), part));
    }

    @WrapOperation(
            method = {
                    "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V",
                    "hasEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Z"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/LevelEntityGetter;get(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V"))
    private <U extends Entity> void exotic_worlds$typedEntitiesThroughSeam(LevelEntityGetter<Entity> entities,
            EntityTypeTest<Entity, U> type, AABB box, AbortableIterationConsumer<U> output, Operation<Void> original) {
        SeamEntitySearch.forEachUntilAborted(exotic_worlds$getFold(), box, output,
                (piece, sink) -> original.call(entities, type, piece, sink));
    }

    @Unique
    private BlockPos exotic_worlds$wrap(BlockPos pos) {
        return exotic_worlds$getFold().fold(pos);
    }

    @Override
    public WorldFold exotic_worlds$getFold() {
        if (this.exotic_worlds$fold == null) {
            this.exotic_worlds$fold = exotic_worlds$resolveFold();
        }

        return this.exotic_worlds$fold;
    }

    @Override
    public List<GenerationFeature> exotic_worlds$getActiveFeatures() {
        if (this.exotic_worlds$activeFeatures == null) {
            ServerLevel level = WorldLoopAttachments.getServerLevel((Level) (Object) this);
            this.exotic_worlds$activeFeatures = level == (Object) this ? ShapeFeatures.installedOn(level) : List.of();
        }

        return this.exotic_worlds$activeFeatures;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> S exotic_worlds$featureState(GenerationFeature feature, Supplier<S> created) {
        return (S) this.exotic_worlds$featureStates.computeIfAbsent(feature.getKey(), key -> created.get());
    }

    @Override
    public AtomicReference<SeamVerdict> exotic_worlds$getSeamVerdict() {
        return this.exotic_worlds$seamVerdict;
    }

    @Override
    public AtomicReference<SeamVerdict> exotic_worlds$getFullSeamVerdict() {
        return this.exotic_worlds$fullSeamVerdict;
    }

    @Unique
    private WorldFold exotic_worlds$resolveFold() {
        ServerLevel level = WorldLoopAttachments.getServerLevel((Level) (Object) this);
        if (level == null) {
            return WorldFolds.NOOP;
        }

        return level == (Object) this
                ? GeneratorShapes.getFold(level.getChunkSource().getGenerator())
                : WorldLoopAttachments.getFold(level);
    }

    @WrapMethod(method = "precipitationAt")
    private Biome.Precipitation exotic_worlds$bindPrecipitationFold(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        WorldFold fold = WorldLoopAttachments.getNoiseFold((Level) (Object) this);
        try (GenerationFoldContext.Context.FoldScope _ = GenerationFoldContext.getContext().withFold(fold)) {
            return original.call(pos);
        }
    }
}
