package com.exoticworlds.compat.seedviewer.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.compat.seedviewer.CreationShape;
import com.exoticworlds.compat.seedviewer.FoldedBiomeSampler;
import com.exoticworlds.compat.seedviewer.FoldedSampleCache;
import com.exoticworlds.compat.seedviewer.SeedViewerInjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.acenia.seedviewer.client.worldgen.BiomeSampler;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

@Mixin(BiomeSampler.class)
public class BiomeSamplerMixin implements FoldedBiomeSampler {
    @Unique
    private WorldFold exotic_worlds$fold;

    @Unique
    private @Nullable FoldedSampleCache<Holder<Biome>> exotic_worlds$cache;

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_BIOME_SOURCE,
            at = @At("RETURN"))
    private void exotic_worlds$takeTheRouterBuildsFold(CallbackInfo callback) {
        exotic_worlds$bind(CreationShape.routerBuildFold());
    }

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_LEVEL,
            at = @At("RETURN"))
    private void exotic_worlds$takeTheLevelsFold(ServerLevel level, CallbackInfo callback) {
        exotic_worlds$bind(WorldLoopAttachments.transformerOf(level));
    }

    @WrapMethod(method = "sample", order = BinderOrder.FOLD)
    private Holder<Biome> exotic_worlds$sampleTheFoldedField(int blockX, int blockY, int blockZ,
            Operation<Holder<Biome>> original) {
        FoldedSampleCache<Holder<Biome>> cache = this.exotic_worlds$cache;
        if (cache == null) {
            return original.call(blockX, blockY, blockZ);
        }

        return cache.get(blockX, blockY, blockZ, () -> GenerationTransformerContext.withTransformer(this.exotic_worlds$fold,
                () -> original.call(blockX, blockY, blockZ)));
    }

    @Override
    public WorldFold exotic_worlds$fold() {
        return this.exotic_worlds$fold;
    }

    @Unique
    private void exotic_worlds$bind(WorldFold fold) {
        this.exotic_worlds$fold = fold;
        this.exotic_worlds$cache = fold.isWrapped() ? FoldedSampleCache.perQuart(fold) : null;
    }
}
