package com.exoticworlds.compat.seedviewer.mixin;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.compat.seedviewer.CreationShape;
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
public class BiomeSamplerMixin {
    @Unique
    private WorldFold toroidal$fold;

    @Unique
    private @Nullable FoldedSampleCache<Holder<Biome>> toroidal$cache;

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_BIOME_SOURCE,
            at = @At("RETURN"))
    private void toroidal$takeTheRouterBuildsFold(CallbackInfo callback) {
        toroidal$bind(CreationShape.routerBuildFold());
    }

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_LEVEL,
            at = @At("RETURN"))
    private void toroidal$takeTheLevelsFold(ServerLevel level, CallbackInfo callback) {
        toroidal$bind(WorldLoopAttachments.transformerOf(level));
    }

    @WrapMethod(method = "sample", order = BinderOrder.FOLD)
    private Holder<Biome> toroidal$sampleTheFoldedField(int blockX, int blockY, int blockZ,
            Operation<Holder<Biome>> original) {
        FoldedSampleCache<Holder<Biome>> cache = this.toroidal$cache;
        Supplier<Holder<Biome>> evaluate = () -> GenerationTransformerContext.withTransformer(this.toroidal$fold,
                () -> original.call(blockX, blockY, blockZ));
        return cache == null ? evaluate.get() : cache.get(blockX, blockY, blockZ, evaluate);
    }

    @Unique
    private void toroidal$bind(WorldFold fold) {
        this.toroidal$fold = fold;
        this.toroidal$cache = fold.isWrapped() ? FoldedSampleCache.perQuart(fold) : null;
    }
}
