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
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.Direction;
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
    private @Nullable WorldFold toroidal$transformer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$registerLockOwner(CallbackInfo ci) {
        if (!this.isClientSide) {
            LightLockFolds.register(this.instanceId, this.lightEngine, this);
        }
    }

    @ModifyVariable(method = "getAnyChunkNow", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapChunkX(int chunkX) {
        return toroidal$transformer().chunkDomain(Direction.Axis.X).wrap(chunkX);
    }

    @ModifyVariable(method = "getAnyChunkNow", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapChunkZ(int chunkZ) {
        return toroidal$transformer().chunkDomain(Direction.Axis.Z).wrap(chunkZ);
    }

    @Override
    public @Nullable WorldFold toroidal$wrappedTransformer() {
        WorldFold transformer = toroidal$transformer();
        return transformer.isWrapped() ? transformer : null;
    }

    @Unique
    private WorldFold toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = this.world == null
                    ? WorldFolds.NOOP
                    : WorldLoopAttachments.transformerOf(this.world);
        }

        return this.toroidal$transformer;
    }
}
