package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin implements TransformerHolder {
    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$bindLevel(CallbackInfo ci) {
        this.exotic_worlds$bound.bind(this.chunkSource.getLevel());
    }

    @ModifyVariable(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapLightValuePos(BlockPos pos) {
        return exotic_worlds$transformer().fold(pos);
    }

    @Override
    public WorldFold exotic_worlds$transformer() {
        return this.exotic_worlds$bound.fold();
    }
}
