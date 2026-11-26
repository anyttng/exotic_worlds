package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.accessors.LightStepFrame;
import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin implements FoldHolder, LightStepFrame {
    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Unique
    private FoldOrientation exotic_worlds$stepOrientation = FoldOrientation.IDENTITY;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$bindLevel(CallbackInfo ci) {
        this.exotic_worlds$bound.bind(this.chunkSource.getLevel());
    }

    @ModifyVariable(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapLightValuePos(BlockPos pos) {
        return exotic_worlds$getFold().fold(pos);
    }

    @Override
    public WorldFold exotic_worlds$getFold() {
        return this.exotic_worlds$bound.getFold();
    }

    @Override
    public FoldOrientation exotic_worlds$getStepOrientation() {
        return this.exotic_worlds$stepOrientation;
    }

    @Override
    public void exotic_worlds$setStepOrientation(FoldOrientation orientation) {
        this.exotic_worlds$stepOrientation = orientation;
    }
}
