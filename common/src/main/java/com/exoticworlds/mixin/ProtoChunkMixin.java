package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.accessors.TerrainMaskHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.ChunkFrame;
import com.exoticworlds.engine.fold.ChunkSeat;
import com.exoticworlds.engine.gen.terrain.TerrainMask;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin implements TerrainMaskHolder {
    @Unique
    private static final String SET_BLOCK_STATE =
            "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)"
                    + "Lnet/minecraft/world/level/block/state/BlockState;";

    @Unique
    private @Nullable TerrainMask exotic_worlds$terrainMask;

    @Override
    public @Nullable TerrainMask exotic_worlds$terrainMask() {
        return this.exotic_worlds$terrainMask;
    }

    @Override
    public void exotic_worlds$terrainMask(TerrainMask mask) {
        this.exotic_worlds$terrainMask = mask;
    }

    @Unique
    private WorldFold exotic_worlds$fold() {
        return WorldLoopAttachments.foldIfLevel(((ChunkAccessAccessor) this).exotic_worlds$getLevelHeightAccessor());
    }

    @WrapMethod(method = "getBlockState")
    private BlockState exotic_worlds$readInOwnFrame(BlockPos pos, Operation<BlockState> original) {
        ChunkPos own = ((ProtoChunk) (Object) this).getPos();
        if (ChunkSeat.sitsIn(own, pos.getX(), pos.getZ())) {
            return original.call(pos);
        }

        WorldFold.Folded<BlockPos> seated = ChunkFrame.seat(this.exotic_worlds$fold(), own, pos);
        return seated == null
                ? original.call(pos)
                : ChunkFrame.toReader(original.call(seated.value()), seated.orientation());
    }

    // Must stay above markWritten: Mixin runs them in declaration order, and the terrain mask takes the seated position.
    @ModifyVariable(
            method = {SET_BLOCK_STATE, "getFluidState", "markPosForPostprocessing"},
            at = @At("HEAD"), argsOnly = true, require = 3)
    private BlockPos exotic_worlds$seatInOwnFrame(BlockPos pos) {
        ChunkPos own = ((ProtoChunk) (Object) this).getPos();
        if (ChunkSeat.sitsIn(own, pos.getX(), pos.getZ())) {
            return pos;
        }

        return ChunkFrame.seatPos(this.exotic_worlds$fold(), own, pos);
    }

    @Inject(method = SET_BLOCK_STATE, at = @At("HEAD"))
    private void exotic_worlds$markWritten(BlockPos pos, BlockState state, int flags,
            CallbackInfoReturnable<@Nullable BlockState> callback) {
        TerrainMask mask = this.exotic_worlds$terrainMask;
        if (mask != null) {
            mask.wrote(pos);
        }
    }
}
