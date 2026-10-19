package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.CrumbSweepCache;
import com.exoticworlds.accessors.PeriodicityMark;
import com.exoticworlds.accessors.TerrainMaskCache;
import com.exoticworlds.accessors.TransformerCache;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.BlockEntitySeat;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.fold.SeamEntitySearch;
import com.exoticworlds.engine.gen.FloatingCrumbs;
import com.exoticworlds.engine.gen.TerrainMasks;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

@Mixin(Level.class)
public class LevelMixin implements TransformerCache, CrumbSweepCache, TerrainMaskCache, PeriodicityMark {
    @Unique
    private static final String exotic_worlds$GET_ENTITIES =
            "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;";

    @Unique
    private WorldFold exotic_worlds$transformer;

    @Unique
    private @Nullable Boolean exotic_worlds$sweepsCrumbs;

    @Unique
    private final TerrainMasks exotic_worlds$terrainMasks = new TerrainMasks();

    @Unique
    private final AtomicBoolean exotic_worlds$periodicityClaimed = new AtomicBoolean();

    @WrapOperation(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkSource;getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private @Nullable ChunkAccess exotic_worlds$wrapChunkAccess(ChunkSource chunkSource, int chunkX, int chunkZ,
            ChunkStatus status, boolean loadOrGenerate, Operation<@Nullable ChunkAccess> original) {
        WorldFold transformer = exotic_worlds$transformer();
        if (!transformer.isWrapped()) {
            return original.call(chunkSource, chunkX, chunkZ, status, loadOrGenerate);
        }

        long folded = transformer.foldChunkKey(ChunkPos.pack(chunkX, chunkZ));
        return original.call(chunkSource, ChunkPos.getX(folded), ChunkPos.getZ(folded),
                status, loadOrGenerate);
    }

    @ModifyVariable(method = "getBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapBlockEntityPos(BlockPos pos) {
        return exotic_worlds$wrap(pos);
    }

    @ModifyVariable(method = "removeBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapRemovedBlockEntityPos(BlockPos pos) {
        return exotic_worlds$wrap(pos);
    }

    @ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapPlacedBlockPos(BlockPos pos) {
        return exotic_worlds$wrap(pos);
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void exotic_worlds$wrapBlockEntityIdentity(BlockEntity blockEntity, CallbackInfo ci) {
        BlockEntitySeat.intoBounds(exotic_worlds$transformer(), blockEntity);
    }

    @WrapOperation(
            method = exotic_worlds$GET_ENTITIES,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/LevelEntityGetter;get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V"))
    private void exotic_worlds$entitiesThroughSeam(LevelEntityGetter<Entity> entities, AABB box, Consumer<Entity> output,
            Operation<Void> original) {
        SeamEntitySearch.forEach(exotic_worlds$transformer(), box, output,
                (piece, sink) -> original.call(entities, piece, sink));
    }

    @WrapOperation(
            method = exotic_worlds$GET_ENTITIES,
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean exotic_worlds$partBoxTowardQuery(AABB query, AABB part, Operation<Boolean> original) {
        return original.call(query, FoldedBoxQuery.toward(exotic_worlds$transformer(), query.getCenter(), part));
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
        SeamEntitySearch.forEachUntilAborted(exotic_worlds$transformer(), box, output,
                (piece, sink) -> original.call(entities, type, piece, sink));
    }

    @Unique
    private BlockPos exotic_worlds$wrap(BlockPos pos) {
        return exotic_worlds$transformer().fold(pos);
    }

    @Override
    public WorldFold exotic_worlds$transformer() {
        if (this.exotic_worlds$transformer == null) {
            this.exotic_worlds$transformer = exotic_worlds$resolveTransformer();
        }

        return this.exotic_worlds$transformer;
    }

    @Override
    public boolean exotic_worlds$sweepsCrumbs() {
        if (this.exotic_worlds$sweepsCrumbs == null) {
            ServerLevel level = WorldLoopAttachments.serverLevelOf((Level) (Object) this);
            this.exotic_worlds$sweepsCrumbs = level == (Object) this && FloatingCrumbs.installedOn(level);
        }

        return this.exotic_worlds$sweepsCrumbs;
    }

    @Override
    public TerrainMasks exotic_worlds$terrainMasks() {
        return this.exotic_worlds$terrainMasks;
    }

    @Override
    public boolean exotic_worlds$claimPeriodicityCheck() {
        return this.exotic_worlds$periodicityClaimed.compareAndSet(false, true);
    }

    @Unique
    private WorldFold exotic_worlds$resolveTransformer() {
        ServerLevel level = WorldLoopAttachments.serverLevelOf((Level) (Object) this);
        if (level == null) {
            return WorldFolds.NOOP;
        }

        return level == (Object) this
                ? ShapedChunkGenerator.transformerOf(level.getChunkSource().getGenerator())
                : WorldLoopAttachments.transformerOf(level);
    }

    @WrapMethod(method = "precipitationAt")
    private Biome.Precipitation exotic_worlds$bindPrecipitationTransformer(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        return GenerationTransformerContext.withTransformer(
                WorldLoopAttachments.noiseTransformerOf((Level) (Object) this), () -> original.call(pos));
    }
}
