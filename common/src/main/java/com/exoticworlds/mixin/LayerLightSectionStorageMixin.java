package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;

@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageMixin {
    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @Unique
    private final BoundLevelFold toroidal$bound = new BoundLevelFold();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$bindLevel(CallbackInfo ci) {
        this.toroidal$bound.bind(this.chunkSource.getLevel());
    }

    @WrapOperation(
            method = "updateSectionStatus",
            at = @At(value = "INVOKE", target = InjectionTargets.SECTION_POS_OFFSET))
    private long toroidal$foldNeighborSection(long sectionNode, int stepX, int stepY, int stepZ,
            Operation<Long> original) {
        return this.toroidal$bound.fold().foldSectionNode(original.call(sectionNode, stepX, stepY, stepZ));
    }
}
