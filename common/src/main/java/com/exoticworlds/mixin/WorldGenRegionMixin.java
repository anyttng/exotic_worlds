package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.accessors.LevelHolder;
import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.fold.ChunkFrame;
import com.exoticworlds.engine.gen.ChunkFrameTicks;
import com.exoticworlds.engine.gen.RegionReach;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.LevelTickAccess;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements LevelHolder {
    @Unique
    private static final String NEW_BLOCK_ENTITY =
            "Lnet/minecraft/world/level/block/EntityBlock;newBlockEntity(Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/entity/BlockEntity;";

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private ChunkAccess center;

    @Shadow
    @Final
    private ChunkStep generatingStep;

    @Unique
    private @Nullable LevelTickAccess<Block> exotic_worlds$blockTicks;

    @Unique
    private @Nullable LevelTickAccess<Fluid> exotic_worlds$fluidTicks;

    @Override
    public ServerLevel exotic_worlds$getLevel() {
        return this.level;
    }

    @Unique
    private @Nullable ChunkPos exotic_worlds$nearestPastTheReach(int chunkX, int chunkZ) {
        return RegionReach.nearestPastTheReach(WorldLoopAttachments.getWrappedFold(this.level),
                this.center.getPos(), this.generatingStep.directDependencies().size(), chunkX, chunkZ);
    }

    @WrapMethod(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;")
    private @Nullable ChunkAccess exotic_worlds$chunkNearestTheGeneratingChunk(int chunkX, int chunkZ,
            ChunkStatus targetStatus, boolean loadOrGenerate, Operation<@Nullable ChunkAccess> original) {
        ChunkPos nearest = this.exotic_worlds$nearestPastTheReach(chunkX, chunkZ);
        return nearest == null
                ? original.call(chunkX, chunkZ, targetStatus, loadOrGenerate)
                : original.call(nearest.x(), nearest.z(), targetStatus, loadOrGenerate);
    }

    @WrapMethod(method = "isWithinWriteZone(II)Z")
    private boolean exotic_worlds$writeZoneThroughTheSeam(int chunkX, int chunkZ, Operation<Boolean> original) {
        ChunkPos nearest = this.exotic_worlds$nearestPastTheReach(chunkX, chunkZ);
        return nearest == null
                ? original.call(chunkX, chunkZ)
                : original.call(nearest.x(), nearest.z());
    }

    @WrapMethod(method = "ensureCanWrite")
    private boolean exotic_worlds$writeGateThroughTheSeam(BlockPos pos, Operation<Boolean> original) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(this.level);
        ChunkPos raw = ChunkPos.containing(pos);
        return fold == null || this.exotic_worlds$nearestPastTheReach(raw.x(), raw.z()) == null
                ? original.call(pos)
                : original.call(fold.nearestCopyTransformation(this.center.getPos(), raw).apply(pos));
    }

    @WrapMethod(method = "hasChunk")
    private boolean exotic_worlds$holdsTheChunkNearestTheGeneratingChunk(int chunkX, int chunkZ,
            Operation<Boolean> original) {
        ChunkPos nearest = this.exotic_worlds$nearestPastTheReach(chunkX, chunkZ);
        return nearest == null
                ? original.call(chunkX, chunkZ)
                : original.call(nearest.x(), nearest.z());
    }

    @Unique
    private DeckTransformation exotic_worlds$slotMove(int chunkX, int chunkZ) {
        return RegionReach.moveToAnsweredChunk(WorldLoopAttachments.getWrappedFold(this.level),
                this.center.getPos(), this.generatingStep.directDependencies().size(), chunkX, chunkZ);
    }

    @Unique
    private DeckTransformation exotic_worlds$moveInto(ChunkAccess chunk, int blockX, int blockZ) {
        ChunkPos raw = new ChunkPos(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
        DeckTransformation move = this.exotic_worlds$slotMove(raw.x(), raw.z());
        return move.isIdentity() || !move.apply(raw).equals(chunk.getPos()) ? DeckTransformation.IDENTITY : move;
    }

    @Unique
    private DeckTransformation exotic_worlds$cellMoveInto(ChunkAccess chunk, int blockX, int blockZ) {
        return WorldLoopAttachments.getFold(this.level).isPreservingLocalIndices()
                ? DeckTransformation.IDENTITY
                : this.exotic_worlds$moveInto(chunk, blockX, blockZ);
    }

    @Unique
    private BlockPos exotic_worlds$keyIn(ChunkAccess chunk, BlockPos pos) {
        return this.exotic_worlds$moveInto(chunk, pos.getX(), pos.getZ()).apply(pos);
    }

    @Unique
    private BlockPos exotic_worlds$cellIn(ChunkAccess chunk, BlockPos pos) {
        return this.exotic_worlds$cellMoveInto(chunk, pos.getX(), pos.getZ()).apply(pos);
    }

    @ModifyVariable(method = "setBlock", at = @At("HEAD"), argsOnly = true)
    private BlockState exotic_worlds$turnPlacedState(BlockState state, @Local(argsOnly = true) BlockPos pos) {
        if (WorldLoopAttachments.getFold(this.level).isPreservingLocalIndices()) {
            return state;
        }

        DeckTransformation move = this.exotic_worlds$slotMove(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        return ChunkFrame.toChunk(state, move.getOrientation());
    }

    @WrapOperation(
            method = {"getBlockState", "getBlockEntity"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState exotic_worlds$readStateInChunkFrame(ChunkAccess chunk, BlockPos pos,
            Operation<BlockState> original) {
        DeckTransformation move = this.exotic_worlds$cellMoveInto(chunk, pos.getX(), pos.getZ());
        return move.isIdentity()
                ? original.call(chunk, pos)
                : ChunkFrame.toReader(original.call(chunk, move.apply(pos)), move.getOrientation());
    }

    @WrapOperation(
            method = "getFluidState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState exotic_worlds$readFluidInChunkFrame(ChunkAccess chunk, BlockPos pos,
            Operation<FluidState> original) {
        return original.call(chunk, this.exotic_worlds$cellIn(chunk, pos));
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;"))
    private @Nullable BlockState exotic_worlds$writeStateInChunkFrame(ChunkAccess chunk, BlockPos pos,
            BlockState state, int flags, Operation<@Nullable BlockState> original) {
        return original.call(chunk, this.exotic_worlds$cellIn(chunk, pos), state, flags);
    }

    @WrapOperation(
            method = "markPosForPostProcessing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;markPosForPostProcessing(Lnet/minecraft/core/BlockPos;)V"))
    private void exotic_worlds$markInChunkFrame(ChunkAccess chunk, BlockPos pos, Operation<Void> original) {
        original.call(chunk, this.exotic_worlds$cellIn(chunk, pos));
    }

    @WrapOperation(
            method = "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int exotic_worlds$heightInChunkFrame(ChunkAccess chunk, Heightmap.Types type, int localX, int localZ,
            Operation<Integer> original, @Local(argsOnly = true, ordinal = 0) int x,
            @Local(argsOnly = true, ordinal = 1) int z) {
        DeckTransformation move = this.exotic_worlds$cellMoveInto(chunk, x, z);
        if (move.isIdentity()) {
            return original.call(chunk, type, localX, localZ);
        }

        BlockPos seated = move.apply(new BlockPos(x, 0, z));
        return original.call(chunk, type,
                SectionPos.sectionRelative(seated.getX()), SectionPos.sectionRelative(seated.getZ()));
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(value = "INVOKE", target = NEW_BLOCK_ENTITY))
    private @Nullable BlockEntity exotic_worlds$createBlockEntityInChunkFrame(
            EntityBlock block, BlockPos pos, BlockState state, Operation<BlockEntity> original, @Local ChunkAccess chunk) {
        return original.call(block, this.exotic_worlds$keyIn(chunk, pos), state);
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V"))
    private void exotic_worlds$removeBlockEntityInChunkFrame(ChunkAccess chunk, BlockPos pos, Operation<Void> original) {
        original.call(chunk, this.exotic_worlds$keyIn(chunk, pos));
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockEntityNbt(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void exotic_worlds$storeNbtInChunkFrame(ChunkAccess chunk, CompoundTag tag, Operation<Void> original, @Local BlockPos pos) {
        BlockPos key = this.exotic_worlds$keyIn(chunk, pos);
        tag.putInt("x", key.getX());
        tag.putInt("z", key.getZ());
        original.call(chunk, tag);
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;updatePOIOnBlockStateChange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void exotic_worlds$updatePoiInChunkFrame(
            ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState, Operation<Void> original, @Local ChunkAccess chunk) {
        original.call(level, this.exotic_worlds$keyIn(chunk, pos), oldState, newState);
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity exotic_worlds$readBlockEntityInChunkFrame(
            ChunkAccess chunk, BlockPos pos, Operation<BlockEntity> original) {
        return original.call(chunk, this.exotic_worlds$keyIn(chunk, pos));
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntityNbt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/nbt/CompoundTag;"))
    private @Nullable CompoundTag exotic_worlds$readBlockEntityNbtInChunkFrame(
            ChunkAccess chunk, BlockPos pos, Operation<CompoundTag> original) {
        return original.call(chunk, this.exotic_worlds$keyIn(chunk, pos));
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(value = "INVOKE", target = NEW_BLOCK_ENTITY))
    private @Nullable BlockEntity exotic_worlds$rebuildBlockEntityInChunkFrame(
            EntityBlock block, BlockPos pos, BlockState state, Operation<BlockEntity> original, @Local ChunkAccess chunk) {
        return original.call(block, this.exotic_worlds$keyIn(chunk, pos), state);
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadStatic(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity exotic_worlds$loadBlockEntityInChunkFrame(
            BlockPos pos,
            BlockState state,
            CompoundTag tag,
            HolderLookup.Provider registries,
            Operation<BlockEntity> original,
            @Local ChunkAccess chunk) {
        return original.call(this.exotic_worlds$keyIn(chunk, pos), state, tag, registries);
    }

    @ModifyReturnValue(method = "getBlockTicks", at = @At("RETURN"))
    private LevelTickAccess<Block> exotic_worlds$blockTicksInChunkFrame(LevelTickAccess<Block> ticks) {
        if (WorldLoopAttachments.getWrappedFold(this.level) == null) {
            return ticks;
        }

        if (this.exotic_worlds$blockTicks == null) {
            this.exotic_worlds$blockTicks = this.exotic_worlds$ticksInChunkFrame(ticks);
        }

        return this.exotic_worlds$blockTicks;
    }

    @ModifyReturnValue(method = "getFluidTicks", at = @At("RETURN"))
    private LevelTickAccess<Fluid> exotic_worlds$fluidTicksInChunkFrame(LevelTickAccess<Fluid> ticks) {
        if (WorldLoopAttachments.getWrappedFold(this.level) == null) {
            return ticks;
        }

        if (this.exotic_worlds$fluidTicks == null) {
            this.exotic_worlds$fluidTicks = this.exotic_worlds$ticksInChunkFrame(ticks);
        }

        return this.exotic_worlds$fluidTicks;
    }

    @Unique
    private <T> LevelTickAccess<T> exotic_worlds$ticksInChunkFrame(LevelTickAccess<T> ticks) {
        WorldGenRegion region = (WorldGenRegion) (Object) this;
        return new ChunkFrameTicks<>(ticks, pos -> this.exotic_worlds$keyIn(region.getChunk(pos), pos));
    }

    @WrapOperation(
            method = "addFreshEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;addEntity(Lnet/minecraft/world/entity/Entity;)V"))
    private void exotic_worlds$addEntityInChunkFrame(ChunkAccess chunk, Entity entity, Operation<Void> original) {
        DeckTransformation move = this.exotic_worlds$moveInto(chunk, entity.getBlockX(), entity.getBlockZ());
        if (!move.isIdentity()) {
            entity.setPos(move.apply(entity.position()));
        }

        original.call(chunk, entity);
    }
}
