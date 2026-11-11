package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.CrumbSweepCache;
import com.exoticworlds.accessors.PeriodicityMark;
import com.exoticworlds.accessors.TerrainMaskCache;
import com.exoticworlds.accessors.FoldCache;
import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.ForeignFrame;
import com.exoticworlds.core.ForeignFrames;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.fold.BlockEntitySeat;
import com.exoticworlds.engine.fold.SeamEntitySearch;
import com.exoticworlds.engine.gen.FloatingCrumbs;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.engine.gen.TerrainMasks;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.SeamVerdict;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

@Mixin(Level.class)
public class LevelMixin implements FoldCache, CrumbSweepCache, TerrainMaskCache, PeriodicityMark {
    @Unique
    private static final String GET_CHUNK =
            "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;";

    @Unique
    private static final String SOURCE_GET_CHUNK = "Lnet/minecraft/world/level/chunk/ChunkSource;"
            + "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;";

    @Unique
    private WorldFold exotic_worlds$fold;

    @Unique
    private @Nullable Boolean exotic_worlds$sweepsCrumbs;

    @Unique
    private final TerrainMasks exotic_worlds$terrainMasks = new TerrainMasks();

    @Unique
    private final AtomicReference<SeamVerdict> exotic_worlds$seamVerdict = new AtomicReference<>();

    // Must stay above the Z handler: Mixin runs them in declaration order, and X folds by the unfolded Z.
    @ModifyArg(method = GET_CHUNK, at = @At(value = "INVOKE", target = SOURCE_GET_CHUNK), index = 0)
    private int exotic_worlds$chunkXThroughSeam(int chunkX, int chunkZ, ChunkStatus status, boolean loadOrGenerate) {
        WorldFold fold = exotic_worlds$fold();
        return fold.isWrapped() ? ChunkPos.getX(fold.foldChunkKey(ChunkPos.asLong(chunkX, chunkZ))) : chunkX;
    }

    @ModifyArg(method = GET_CHUNK, at = @At(value = "INVOKE", target = SOURCE_GET_CHUNK), index = 1)
    private int exotic_worlds$chunkZThroughSeam(int chunkX, int chunkZ, ChunkStatus status, boolean loadOrGenerate) {
        WorldFold fold = exotic_worlds$fold();
        return fold.isWrapped() ? ChunkPos.getZ(fold.foldChunkKey(ChunkPos.asLong(chunkX, chunkZ))) : chunkZ;
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
        BlockEntitySeat.intoBounds(exotic_worlds$fold(), blockEntity);
    }

    @WrapOperation(
            method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.LEVEL_ENTITY_GETTER_GET))
    private void exotic_worlds$entitiesThroughSeam(LevelEntityGetter<Entity> entities, AABB box, Consumer<Entity> output,
            Operation<Void> original) {
        SeamEntitySearch.forEach(exotic_worlds$fold(), box, output,
                (piece, sink) -> original.call(entities, piece, sink));
    }

    @WrapOperation(
            method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.LEVEL_ENTITY_GETTER_GET_TYPED))
    private <U extends Entity> void exotic_worlds$typedEntitiesThroughSeam(LevelEntityGetter<Entity> entities,
            EntityTypeTest<Entity, U> type, AABB box, AbortableIterationConsumer<U> output, Operation<Void> original) {
        SeamEntitySearch.forEachUntilAborted(exotic_worlds$fold(), box, output,
                (piece, sink) -> original.call(entities, type, piece, sink));
    }

    @Unique
    private BlockPos exotic_worlds$wrap(BlockPos pos) {
        return exotic_worlds$fold().fold(pos);
    }

    @Inject(method = "getWorldBorder", at = @At("RETURN"))
    private void exotic_worlds$bindBorderToLevelShape(CallbackInfoReturnable<WorldBorder> cir) {
        WorldFold fold = exotic_worlds$fold();
        FoldHolder border = (FoldHolder) cir.getReturnValue();
        if (border.exotic_worlds$fold() != fold) {
            border.exotic_worlds$setFold(fold);
        }
    }

    @Override
    public WorldFold exotic_worlds$fold() {
        if (this.exotic_worlds$fold == null) {
            this.exotic_worlds$fold = exotic_worlds$resolveFold();
        }

        return this.exotic_worlds$fold;
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
    public AtomicReference<SeamVerdict> exotic_worlds$seamVerdict() {
        return this.exotic_worlds$seamVerdict;
    }

    @Unique
    private WorldFold exotic_worlds$resolveFold() {
        ServerLevel level = WorldLoopAttachments.serverLevelOf((Level) (Object) this);
        if (level == null) {
            return WorldFolds.NOOP;
        }

        return level == (Object) this
                ? exotic_worlds$generatorFold(level)
                : WorldLoopAttachments.foldOf(level);
    }

    @Unique
    private static WorldFold exotic_worlds$generatorFold(ServerLevel level) {
        CarriedShape carried = ShapedChunkGenerator.carriedShapeOf(level.getChunkSource().getGenerator());
        if (carried == null) {
            return WorldFolds.NOOP;
        }

        List<ForeignFrame> foreignFrames = ForeignFrames.of(level);
        return foreignFrames.isEmpty()
                ? carried.fold()
                : WorldFolds.of(carried.shape(), foreignFrames, carried.generationOptions());
    }

    // Whether rain falls on a block is the same temperature field the ice is placed from, asked outside any
    // generation step — so the fold has to be bound here too, or the sky disagrees with the ground at the seam.
    // On 1.21.1 the level-side primitive is isRainingAt; 26.x later split its body out as precipitationAt.
    // On a client level this binds NOOP, which is correct twice over: the client's engine fold is NOOP by
    // design, and a bound NOOP shields the call from a leftover binding on the same thread.
    @WrapMethod(method = "isRainingAt")
    private boolean exotic_worlds$bindPrecipitationFold(BlockPos pos, Operation<Boolean> original) {
        WorldFold fold = WorldLoopAttachments.noiseFoldOf((Level) (Object) this);
        try (GenerationFoldContext.Context.FoldScope scope = GenerationFoldContext.context().withFold(fold)) {
            return original.call(pos);
        }
    }
}
