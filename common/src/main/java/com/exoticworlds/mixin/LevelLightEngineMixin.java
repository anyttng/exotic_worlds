package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin {
    @Shadow
    @Final
    protected LevelHeightAccessor levelHeightAccessor;

    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$bindLevel(CallbackInfo ci) {
        this.exotic_worlds$bound.bind(this.levelHeightAccessor);
    }

    @Inject(method = "checkBlock", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$skipBlockPastBounds(BlockPos pos, CallbackInfo ci) {
        if (exotic_worlds$pastBounds(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            ci.cancel();
        }
    }

    @Inject(method = "propagateLightSources", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$skipSourcesPastBounds(ChunkPos pos, CallbackInfo ci) {
        if (exotic_worlds$pastBounds(pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "setLightEnabled", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$skipEnablePastBounds(ChunkPos pos, boolean enable, CallbackInfo ci) {
        if (exotic_worlds$pastBounds(pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "retainData", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$skipRetainPastBounds(ChunkPos pos, boolean retain, CallbackInfo ci) {
        if (exotic_worlds$pastBounds(pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateSectionStatus", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$skipSectionPastBounds(SectionPos pos, boolean sectionEmpty, CallbackInfo ci) {
        if (exotic_worlds$pastBounds(pos.x(), pos.z())) {
            ci.cancel();
        }
    }

    @Inject(method = "queueSectionData", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$skipSectionDataPastBounds(LightLayer layer, SectionPos pos, @Nullable DataLayer data,
            CallbackInfo ci) {
        if (exotic_worlds$pastBounds(pos.x(), pos.z())) {
            ci.cancel();
        }
    }

    @Unique
    private boolean exotic_worlds$pastBounds(ChunkPos pos) {
        WorldFold fold = this.exotic_worlds$bound.fold();
        return fold.isWrapped() && fold.isOver(pos);
    }

    @Unique
    private boolean exotic_worlds$pastBounds(int chunkX, int chunkZ) {
        WorldFold fold = this.exotic_worlds$bound.fold();
        return fold.isWrapped() && fold.isOver(new ChunkPos(chunkX, chunkZ));
    }
}
