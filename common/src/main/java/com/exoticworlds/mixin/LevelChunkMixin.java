package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.RelocatableBlockEntity;
import com.exoticworlds.client.engine.SyncedTagFold;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.ChunkSeat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {
    @Shadow
    @Final
    private Level level;

    @ModifyVariable(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)"
                    + "Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapWrittenPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @ModifyVariable(
            method = "getBlockEntity(Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)"
                    + "Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapQueriedPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @ModifyVariable(method = "removeBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapRemovedPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @ModifyVariable(method = "getBlockEntityNbtForSaving", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapSavedPos(BlockPos pos) {
        return toroidal$wrap(pos);
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void toroidal$wrapBlockEntityIdentity(BlockEntity blockEntity, CallbackInfo ci) {
        WorldFold transformer = WorldLoopAttachments.transformerOf(this.level);
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
            method = {"lambda$replaceWithPacketData$3", "method_31716"},
            at = {
                    @At(value = "INVOKE", target = InjectionTargets.BLOCK_ENTITY_HANDLE_UPDATE_TAG),
                    @At(value = "INVOKE", target = InjectionTargets.BLOCK_ENTITY_LOAD_WITH_COMPONENTS)})
    private void toroidal$seatSyncedPositions(BlockEntity blockEntity, CompoundTag tag,
            HolderLookup.Provider registries, Operation<Void> original) {
        original.call(blockEntity, SyncedTagFold.inFrameOf(blockEntity, tag), registries);
    }

    @Unique
    private BlockPos toroidal$wrap(BlockPos pos) {
        WorldFold transformer = WorldLoopAttachments.transformerOf(this.level);
        if (transformer.isWrapped()) {
            return transformer.fold(pos);
        }

        WorldFold clientBounds = WorldLoopAttachments.wrappedClientBoundsTransformerOf(this.level);
        if (clientBounds == null) {
            return pos;
        }

        return ChunkSeat.onto(clientBounds, ((LevelChunk) (Object) this).getPos(), pos);
    }
}
