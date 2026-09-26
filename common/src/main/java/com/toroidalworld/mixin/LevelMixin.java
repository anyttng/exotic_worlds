package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.accessors.CrumbSweepCache;
import com.toroidalworld.accessors.PeriodicityMark;
import com.toroidalworld.accessors.RelocatableBlockEntity;
import com.toroidalworld.accessors.TerrainMaskCache;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.fold.FoldedBoxQuery;
import com.toroidalworld.engine.fold.SeamEntitySearch;
import com.toroidalworld.engine.gen.FloatingCrumbs;
import com.toroidalworld.engine.gen.TerrainMasks;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
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
    private WorldFold toroidal$transformer;

    @Unique
    private @Nullable Boolean toroidal$sweepsCrumbs;

    @Unique
    private @Nullable TerrainMasks toroidal$terrainMasks;

    @Unique
    private final AtomicBoolean toroidal$periodicityClaimed = new AtomicBoolean();

    @WrapOperation(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkSource;getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private @Nullable ChunkAccess toroidal$wrapChunkAccess(ChunkSource chunkSource, int chunkX, int chunkZ,
            ChunkStatus status, boolean loadOrGenerate, Operation<@Nullable ChunkAccess> original) {
        WorldFold transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return original.call(chunkSource, chunkX, chunkZ, status, loadOrGenerate);
        }

        long folded = transformer.foldChunkKey(ChunkPos.pack(chunkX, chunkZ));
        return original.call(chunkSource, ChunkPos.getX(folded), ChunkPos.getZ(folded),
                status, loadOrGenerate);
    }

    @ModifyVariable(method = "getBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapBlockEntityPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @ModifyVariable(method = "removeBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapRemovedBlockEntityPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapPlacedBlockPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void toroidal$wrapBlockEntityIdentity(BlockEntity blockEntity, CallbackInfo ci) {
        WorldFold transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        if (!transformer.isOver(pos)) {
            return;
        }

        ((RelocatableBlockEntity) blockEntity).toroidal$relocate(transformer.fold(pos));
    }

    @WrapOperation(
            method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/LevelEntityGetter;get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V"))
    private void toroidal$entitiesThroughSeam(LevelEntityGetter<Entity> entities, AABB box, Consumer<Entity> output,
            Operation<Void> original) {
        SeamEntitySearch.forEach(toroidal$transformer(), box, output,
                (piece, sink) -> original.call(entities, piece, sink));
    }

    @WrapOperation(
            method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean toroidal$partBoxTowardQuery(AABB query, AABB part, Operation<Boolean> original) {
        return original.call(query, FoldedBoxQuery.toward(toroidal$transformer(), query.getCenter(), part));
    }

    @WrapOperation(
            method = {
                    "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V",
                    "hasEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Z"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/LevelEntityGetter;get(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V"))
    private <U extends Entity> void toroidal$typedEntitiesThroughSeam(LevelEntityGetter<Entity> entities,
            EntityTypeTest<Entity, U> type, AABB box, AbortableIterationConsumer<U> output, Operation<Void> original) {
        SeamEntitySearch.forEachUntilAborted(toroidal$transformer(), box, output,
                (piece, sink) -> original.call(entities, type, piece, sink));
    }

    @Unique
    private BlockPos toroidal$wrap(BlockPos pos) {
        WorldFold transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return pos;
        }

        return transformer.fold(pos);
    }

    @Override
    public WorldFold toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = toroidal$resolveTransformer();
        }

        return this.toroidal$transformer;
    }

    @Override
    public boolean toroidal$sweepsCrumbs() {
        if (this.toroidal$sweepsCrumbs == null) {
            ServerLevel level = WorldLoopAttachments.serverLevelOf((Level) (Object) this);
            this.toroidal$sweepsCrumbs = level == (Object) this && FloatingCrumbs.installedOn(level);
        }

        return this.toroidal$sweepsCrumbs;
    }

    @Override
    public TerrainMasks toroidal$terrainMasks() {
        if (this.toroidal$terrainMasks == null) {
            this.toroidal$terrainMasks = new TerrainMasks();
        }

        return this.toroidal$terrainMasks;
    }

    @Override
    public boolean toroidal$claimPeriodicityCheck() {
        return this.toroidal$periodicityClaimed.compareAndSet(false, true);
    }

    @Unique
    private WorldFold toroidal$resolveTransformer() {
        ServerLevel level = WorldLoopAttachments.serverLevelOf((Level) (Object) this);
        if (level == null) {
            return WorldFolds.NOOP;
        }

        return level == (Object) this
                ? ShapedChunkGenerator.transformerOf(level.getChunkSource().getGenerator())
                : WorldLoopAttachments.transformerOf(level);
    }

    @WrapMethod(method = "precipitationAt")
    private Biome.Precipitation toroidal$bindPrecipitationTransformer(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        return GenerationTransformerContext.withTransformer(
                WorldLoopAttachments.noiseTransformerOf((Level) (Object) this), () -> original.call(pos));
    }
}
