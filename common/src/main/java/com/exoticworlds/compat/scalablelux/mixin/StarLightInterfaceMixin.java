package com.exoticworlds.compat.scalablelux.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.compat.scalablelux.LightLockFolds;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(targets = "ca.spottedleaf.starlight.common.light.StarLightInterface", remap = false)
public class StarLightInterfaceMixin implements TransformerSource {
    @Shadow
    @Final
    protected @Nullable Level world;

    @Shadow
    @Final
    protected boolean isClientSide;

    @Shadow
    @Final
    public LevelLightEngine lightEngine;

    @Shadow
    @Final
    private int instanceId;

    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$registerLockOwner(CallbackInfo ci) {
        this.exotic_worlds$bound.bind(this.world);
        if (!this.isClientSide) {
            LightLockFolds.register(this.instanceId, this.lightEngine, this);
        }
    }

    @ModifyVariable(method = "getAnyChunkNow", at = @At("HEAD"), argsOnly = true, index = 1)
    private int exotic_worlds$wrapChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getAnyChunkNow", at = @At("HEAD"), argsOnly = true, index = 2)
    private int exotic_worlds$wrapChunkZ(int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldZ(chunkZ);
    }

    @Override
    public @Nullable WorldFold exotic_worlds$wrappedTransformer() {
        return this.exotic_worlds$bound.wrapped();
    }
}
