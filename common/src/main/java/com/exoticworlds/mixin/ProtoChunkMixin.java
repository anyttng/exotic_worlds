package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.accessors.TerrainMaskHolder;
import com.exoticworlds.engine.gen.TerrainMask;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin implements TerrainMaskHolder {
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

    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"))
    private void exotic_worlds$markWritten(BlockPos pos, BlockState state, boolean moving,
            CallbackInfoReturnable<@Nullable BlockState> callback) {
        TerrainMask mask = this.exotic_worlds$terrainMask;
        if (mask != null) {
            mask.wrote(pos);
        }
    }
}
