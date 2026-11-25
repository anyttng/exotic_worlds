package com.exoticworlds.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.accessors.FoldedClimateSampler;
import com.exoticworlds.accessors.FoldedCompileSource;
import com.exoticworlds.accessors.FoldedRandomState;
import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.RandomStateFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.gen.GenerationMoments;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.gen.BaseHeights;
import com.exoticworlds.engine.gen.CanonicalRandomFactory;
import com.exoticworlds.engine.noise.AquiferCells;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.NoiseScaleLadder;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(RandomState.class)
public class RandomStateMixin implements CoastLiftCache, FoldSource, FoldedRandomState {
    @Unique
    private static final String exotic_worlds$CONSTRUCTOR = "<init>(Lnet/minecraft/core/HolderGetter;JZ"
            + "Lnet/minecraft/world/level/block/state/BlockState;ILnet/minecraft/world/level/levelgen/NoiseRouter;)V";

    @Unique
    private volatile double exotic_worlds$coastLift;

    @Unique
    private @Nullable WorldFold exotic_worlds$fold;

    @Unique
    private @Nullable FoldedCompileContext exotic_worlds$compileContext;

    @Unique
    private volatile @Nullable AquiferCells exotic_worlds$aquiferCells;

    @Unique
    private @Nullable BaseHeights exotic_worlds$baseHeights;

    @ModifyArg(
            method = exotic_worlds$CONSTRUCTOR,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/material/MaterialSystem;<init>("
                            + "Lnet/minecraft/world/level/levelgen/RandomState;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;I"
                            + "Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunction;"
                            + "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;)V"),
            index = 4)
    private PositionalRandomFactory exotic_worlds$materialRandomFromCanonical(PositionalRandomFactory noiseRandom) {
        WorldFold fold = GenerationFoldContext.getContext().getRouterBuildFold();
        return fold == null ? noiseRandom : new CanonicalRandomFactory(noiseRandom, fold);
    }

    @ModifyArg(
            method = exotic_worlds$CONSTRUCTOR,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunctionCompiler;<init>("
                            + "Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunction$CompileContext;)V"))
    private DensityFunction.CompileContext exotic_worlds$compileWithTheFold(DensityFunction.CompileContext context,
            @Local(argsOnly = true) NoiseRouter router) {
        WorldFold fold = GenerationFoldContext.getContext().getRouterBuildFold();
        if (fold == null) {
            return context;
        }

        this.exotic_worlds$compileContext = new FoldedCompileContext(context, fold, this,
                NoiseScaleLadder.of(fold, exotic_worlds$roots(router), context));
        ((FoldedCompileSource) context).exotic_worlds$setFoldedCompile(this.exotic_worlds$compileContext);
        return context;
    }

    @Inject(method = exotic_worlds$CONSTRUCTOR, at = @At("RETURN"))
    private void exotic_worlds$runGenerationHooks(HolderGetter<NormalNoise> noises, long seed, boolean useLegacyRandom,
            BlockState defaultBlock, int seaLevel, NoiseRouter router, CallbackInfo callback) {
        WorldFold fold = GenerationFoldContext.getContext().getRouterBuildFold();
        this.exotic_worlds$fold = fold;
        if (fold != null) {
            this.exotic_worlds$baseHeights = new BaseHeights(fold);
            RandomState randomState = (RandomState) (Object) this;
            List<GenerationFeature> features = GenerationFoldContext.getContext().getRouterBuildFeatures();
            for (RandomStateFeature feature : ShapeFeatures.atMoment(features, RandomStateFeature.class)) {
                feature.atRandomState(randomState, fold, seaLevel);
            }
            GenerationMoments.runAtRandomState(randomState, fold, seaLevel);
        }
    }

    @ModifyReturnValue(method = "createClimateSampler", at = @At("RETURN"))
    private Climate.Sampler exotic_worlds$stampTheFold(Climate.Sampler sampler) {
        if (this.exotic_worlds$fold != null) {
            ((FoldedClimateSampler) (Object) sampler).exotic_worlds$setFold(this.exotic_worlds$fold);
        }
        return sampler;
    }

    @Unique
    private static List<DensityFunction> exotic_worlds$roots(NoiseRouter router) {
        return List.of(router.temperature(), router.vegetation(), router.continents(), router.erosion(),
                router.depth(), router.ridges(), router.chunkSurfaceLevel(), router.finalDensity());
    }

    @Override
    public @Nullable FoldedCompileContext exotic_worlds$getCompileContext() {
        return this.exotic_worlds$compileContext;
    }

    @Override
    public @Nullable BaseHeights exotic_worlds$getBaseHeights() {
        return this.exotic_worlds$baseHeights;
    }

    @Override
    public @Nullable AquiferCells exotic_worlds$getAquiferCells() {
        return this.exotic_worlds$aquiferCells;
    }

    @Override
    public void exotic_worlds$setAquiferCells(AquiferCells cells) {
        this.exotic_worlds$aquiferCells = cells;
    }

    @Override
    public @Nullable WorldFold exotic_worlds$getWrappedFold() {
        return this.exotic_worlds$fold;
    }

    @Override
    public double exotic_worlds$getCoastLift() {
        return this.exotic_worlds$coastLift;
    }

    @Override
    public void exotic_worlds$setCoastLift(double lift) {
        this.exotic_worlds$coastLift = lift;
    }
}
