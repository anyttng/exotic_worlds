package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.accessors.ChunkWriteListenerHolder;
import com.exoticworlds.core.feature.ChunkWriteListener;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.fold.ChunkFrame;
import com.exoticworlds.engine.fold.ChunkSeat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin implements ChunkWriteListenerHolder {
    @Unique
    private static final String SET_BLOCK_STATE =
            "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)"
                    + "Lnet/minecraft/world/level/block/state/BlockState;";

    @Unique
    private @Nullable ChunkWriteListener exotic_worlds$writeListener;

    @Override
    public @Nullable ChunkWriteListener exotic_worlds$getWriteListener() {
        return this.exotic_worlds$writeListener;
    }

    @Override
    public void exotic_worlds$setWriteListener(ChunkWriteListener listener) {
        this.exotic_worlds$writeListener = listener;
    }

    @Unique
    private WorldFold exotic_worlds$getFold() {
        return WorldLoopAttachments.getFoldIfLevel(((ChunkAccessAccessor) this).exotic_worlds$getLevelHeightAccessor());
    }

    @WrapMethod(method = "getBlockState")
    private BlockState exotic_worlds$readInOwnFrame(BlockPos pos, Operation<BlockState> original) {
        ChunkPos own = ((ProtoChunk) (Object) this).getPos();
        if (ChunkSeat.isSittingIn(own, pos.getX(), pos.getZ())) {
            return original.call(pos);
        }

        WorldFold.Folded<BlockPos> seated = ChunkFrame.seat(this.exotic_worlds$getFold(), own, pos);
        return seated == null
                ? original.call(pos)
                : ChunkFrame.toReader(original.call(seated.value()), seated.orientation());
    }

    // Must stay above markWritten: Mixin runs them in declaration order, and the write listener takes the seated position.
    @ModifyVariable(
            method = {SET_BLOCK_STATE, "getFluidState", "markPosForPostProcessing"},
            at = @At("HEAD"), argsOnly = true, require = 3)
    private BlockPos exotic_worlds$seatInOwnFrame(BlockPos pos) {
        ChunkPos own = ((ProtoChunk) (Object) this).getPos();
        if (ChunkSeat.isSittingIn(own, pos.getX(), pos.getZ())) {
            return pos;
        }

        return ChunkFrame.seatPos(this.exotic_worlds$getFold(), own, pos);
    }

    @Inject(method = SET_BLOCK_STATE, at = @At("HEAD"))
    private void exotic_worlds$markWritten(BlockPos pos, BlockState state, int flags,
            CallbackInfoReturnable<@Nullable BlockState> callback) {
        ChunkWriteListener listener = this.exotic_worlds$writeListener;
        if (listener != null) {
            listener.beforeBlockWrite(pos);
        }
    }
}
