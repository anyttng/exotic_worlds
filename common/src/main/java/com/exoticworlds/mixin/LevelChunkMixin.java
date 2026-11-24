package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.client.engine.SyncedTagFold;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.fold.BlockEntitySeat;
import com.exoticworlds.engine.fold.ChunkFrame;
import com.exoticworlds.engine.fold.ChunkSeat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {
    @Shadow
    @Final
    private Level level;

    @ModifyVariable(
            method = {
                    "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)"
                            + "Lnet/minecraft/world/level/block/state/BlockState;",
                    "getBlockEntity(Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)"
                            + "Lnet/minecraft/world/level/block/entity/BlockEntity;",
                    "removeBlockEntity",
                    "getBlockEntityNbtForSaving"
            },
            at = @At("HEAD"), argsOnly = true, require = 4)
    private BlockPos exotic_worlds$wrapPos(BlockPos pos) {
        WorldFold fold = WorldLoopAttachments.getFold(this.level);
        if (fold.isWrapped()) {
            return fold.fold(pos);
        }

        return ChunkSeat.onto(WorldLoopAttachments.getClientBoundsFold(this.level),
                ((LevelChunk) (Object) this).getPos(), pos);
    }

    @WrapMethod(method = "getBlockState")
    private BlockState exotic_worlds$readInOwnFrame(BlockPos pos, Operation<BlockState> original) {
        ChunkPos own = ((LevelChunk) (Object) this).getPos();
        if (ChunkSeat.isSittingIn(own, pos.getX(), pos.getZ())) {
            return original.call(pos);
        }

        WorldFold.Folded<BlockPos> seated = ChunkFrame.seat(WorldLoopAttachments.getFold(this.level), own, pos);
        return seated == null
                ? original.call(pos)
                : ChunkFrame.toReader(original.call(seated.value()), seated.orientation());
    }

    @WrapMethod(method = "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;")
    private FluidState exotic_worlds$readFluidInOwnFrame(int x, int y, int z, Operation<FluidState> original) {
        ChunkPos own = ((LevelChunk) (Object) this).getPos();
        if (ChunkSeat.isSittingIn(own, x, z)) {
            return original.call(x, y, z);
        }

        long seated = ChunkFrame.seatColumn(WorldLoopAttachments.getFold(this.level), own, x, z);
        return original.call(BlockPos.getX(seated), y, BlockPos.getZ(seated));
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void exotic_worlds$wrapBlockEntityIdentity(BlockEntity blockEntity, CallbackInfo ci) {
        BlockEntitySeat.intoBounds(WorldLoopAttachments.getFold(this.level), blockEntity);
    }

    @WrapOperation(
            method = "lambda$replaceWithPacketData$0",
            at = @At(value = "INVOKE", target = InjectionTargets.TAG_VALUE_INPUT_CREATE))
    private ValueInput exotic_worlds$seatSyncedPositions(ProblemReporter reporter, HolderLookup.Provider registries,
            CompoundTag tag, Operation<ValueInput> original, @Local BlockEntity blockEntity) {
        return original.call(reporter, registries, SyncedTagFold.inFrameOf(blockEntity, tag));
    }
}
