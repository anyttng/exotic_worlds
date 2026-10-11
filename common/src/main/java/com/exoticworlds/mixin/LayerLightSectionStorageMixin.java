package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
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
    private @Nullable WorldFold toroidal$transformer;

    @WrapOperation(
            method = "updateSectionStatus",
            at = @At(value = "INVOKE", target = InjectionTargets.SECTION_POS_OFFSET))
    private long toroidal$foldNeighborSection(long sectionNode, int stepX, int stepY, int stepZ,
            Operation<Long> original) {
        long neighborNode = original.call(sectionNode, stepX, stepY, stepZ);
        WorldFold transformer = toroidal$transformer();
        return transformer.isWrapped() ? transformer.foldSectionNode(neighborNode) : neighborNode;
    }

    @Unique
    private WorldFold toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = WorldLoopAttachments.transformerIfLevel(this.chunkSource.getLevel());
        }

        return this.toroidal$transformer;
    }
}
