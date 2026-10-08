package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.LevelHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.gen.ChunkFrameTicks;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTickAccess;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements LevelHolder {
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
    private @Nullable LevelTickAccess<Block> toroidal$blockTicks;

    @Unique
    private @Nullable LevelTickAccess<Fluid> toroidal$fluidTicks;

    @Override
    public ServerLevel toroidal$level() {
        return this.level;
    }

    @Unique
    private @Nullable ChunkPos toroidal$nearestPastTheReach(int chunkX, int chunkZ) {
        ChunkPos centre = this.center.getPos();
        if (centre.getChessboardDistance(chunkX, chunkZ) < this.generatingStep.directDependencies().size()) {
            return null;
        }

        ChunkPos nearest = NearestCopy.toward(WorldLoopAttachments.wrappedTransformerOf(this.level), centre,
                new ChunkPos(chunkX, chunkZ));
        return nearest.x() == chunkX && nearest.z() == chunkZ ? null : nearest;
    }

    @WrapMethod(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;")
    private @Nullable ChunkAccess toroidal$chunkNearestTheGeneratingChunk(int chunkX, int chunkZ,
            ChunkStatus targetStatus, boolean loadOrGenerate, Operation<@Nullable ChunkAccess> original) {
        ChunkPos nearest = this.toroidal$nearestPastTheReach(chunkX, chunkZ);
        return nearest == null
                ? original.call(chunkX, chunkZ, targetStatus, loadOrGenerate)
                : original.call(nearest.x(), nearest.z(), targetStatus, loadOrGenerate);
    }

    @WrapMethod(method = "ensureCanWrite")
    private boolean toroidal$writeZoneThroughTheSeam(BlockPos pos, Operation<Boolean> original) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        ChunkPos nearest = this.toroidal$nearestPastTheReach(chunkX, chunkZ);
        return nearest == null
                ? original.call(pos)
                : original.call(pos.offset(SectionPos.sectionToBlockCoord(nearest.x() - chunkX), 0,
                        SectionPos.sectionToBlockCoord(nearest.z() - chunkZ)));
    }

    @WrapMethod(method = "hasChunk")
    private boolean toroidal$holdsTheChunkNearestTheGeneratingChunk(int chunkX, int chunkZ,
            Operation<Boolean> original) {
        ChunkPos nearest = this.toroidal$nearestPastTheReach(chunkX, chunkZ);
        return nearest == null
                ? original.call(chunkX, chunkZ)
                : original.call(nearest.x(), nearest.z());
    }

    @Unique
    private BlockPos toroidal$keyIn(ChunkAccess chunk, BlockPos pos) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        return transformer == null ? pos : transformer.reseat(pos, chunk.getPos());
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/EntityBlock;newBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity toroidal$createBlockEntityInChunkFrame(
            EntityBlock block, BlockPos pos, BlockState state, Operation<BlockEntity> original, @Local ChunkAccess chunk) {
        return original.call(block, this.toroidal$keyIn(chunk, pos), state);
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V"))
    private void toroidal$removeBlockEntityInChunkFrame(ChunkAccess chunk, BlockPos pos, Operation<Void> original) {
        original.call(chunk, this.toroidal$keyIn(chunk, pos));
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockEntityNbt(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void toroidal$storeNbtInChunkFrame(ChunkAccess chunk, CompoundTag tag, Operation<Void> original, @Local BlockPos pos) {
        BlockPos key = this.toroidal$keyIn(chunk, pos);
        tag.putInt("x", key.getX());
        tag.putInt("z", key.getZ());
        original.call(chunk, tag);
    }

    @WrapOperation(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;updatePOIOnBlockStateChange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void toroidal$updatePoiInChunkFrame(
            ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState, Operation<Void> original, @Local ChunkAccess chunk) {
        original.call(level, this.toroidal$keyIn(chunk, pos), oldState, newState);
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity toroidal$readBlockEntityInChunkFrame(
            ChunkAccess chunk, BlockPos pos, Operation<BlockEntity> original) {
        return original.call(chunk, this.toroidal$keyIn(chunk, pos));
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockEntityNbt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/nbt/CompoundTag;"))
    private @Nullable CompoundTag toroidal$readBlockEntityNbtInChunkFrame(
            ChunkAccess chunk, BlockPos pos, Operation<CompoundTag> original) {
        return original.call(chunk, this.toroidal$keyIn(chunk, pos));
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/EntityBlock;newBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity toroidal$rebuildBlockEntityInChunkFrame(
            EntityBlock block, BlockPos pos, BlockState state, Operation<BlockEntity> original, @Local ChunkAccess chunk) {
        return original.call(block, this.toroidal$keyIn(chunk, pos), state);
    }

    @WrapOperation(
            method = "getBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadStatic(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private @Nullable BlockEntity toroidal$loadBlockEntityInChunkFrame(
            BlockPos pos,
            BlockState state,
            CompoundTag tag,
            HolderLookup.Provider registries,
            Operation<BlockEntity> original,
            @Local ChunkAccess chunk) {
        return original.call(this.toroidal$keyIn(chunk, pos), state, tag, registries);
    }

    @ModifyReturnValue(method = "getBlockTicks", at = @At("RETURN"))
    private LevelTickAccess<Block> toroidal$blockTicksInChunkFrame(LevelTickAccess<Block> ticks) {
        if (this.toroidal$blockTicks == null) {
            this.toroidal$blockTicks = this.toroidal$ticksInChunkFrame(ticks);
        }

        return this.toroidal$blockTicks;
    }

    @ModifyReturnValue(method = "getFluidTicks", at = @At("RETURN"))
    private LevelTickAccess<Fluid> toroidal$fluidTicksInChunkFrame(LevelTickAccess<Fluid> ticks) {
        if (this.toroidal$fluidTicks == null) {
            this.toroidal$fluidTicks = this.toroidal$ticksInChunkFrame(ticks);
        }

        return this.toroidal$fluidTicks;
    }

    @Unique
    private <T> LevelTickAccess<T> toroidal$ticksInChunkFrame(LevelTickAccess<T> ticks) {
        if (WorldLoopAttachments.wrappedTransformerOf(this.level) == null) {
            return ticks;
        }

        WorldGenRegion region = (WorldGenRegion) (Object) this;
        return new ChunkFrameTicks<>(ticks, pos -> this.toroidal$keyIn(region.getChunk(pos), pos));
    }

    @WrapOperation(
            method = "addFreshEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;addEntity(Lnet/minecraft/world/entity/Entity;)V"))
    private void toroidal$addEntityInChunkFrame(ChunkAccess chunk, Entity entity, Operation<Void> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer != null && transformer.isOver(entity.position())) {
            entity.setPos(transformer.fold(entity.position()));
        }

        original.call(chunk, entity);
    }
}
