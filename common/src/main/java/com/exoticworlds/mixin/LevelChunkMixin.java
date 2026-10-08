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
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {
    @Shadow
    @Final
    private Level level;

    @ModifyVariable(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)"
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
            method = "lambda$replaceWithPacketData$0",
            at = @At(value = "INVOKE", target = InjectionTargets.TAG_VALUE_INPUT_CREATE))
    private ValueInput toroidal$seatSyncedPositions(ProblemReporter reporter, HolderLookup.Provider registries,
            CompoundTag tag, Operation<ValueInput> original, @Local BlockEntity blockEntity) {
        return original.call(reporter, registries, SyncedTagFold.inFrameOf(blockEntity, tag));
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
