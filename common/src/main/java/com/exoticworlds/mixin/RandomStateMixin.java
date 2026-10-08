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
import com.exoticworlds.accessors.FoldedRandomState;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.GenerationMoments;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.gen.CanonicalRandomFactory;
import com.exoticworlds.engine.noise.AquiferCells;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.NoiseScaleLadder;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(RandomState.class)
public class RandomStateMixin implements CoastLiftCache, TransformerSource, FoldedRandomState {
    @Unique
    private static final String toroidal$CONSTRUCTOR = "<init>(Lnet/minecraft/core/HolderGetter;JZ"
            + "Lnet/minecraft/world/level/block/state/BlockState;ILnet/minecraft/world/level/levelgen/NoiseRouter;)V";

    @Unique
    private volatile double toroidal$coastLift;

    @Unique
    private @Nullable WorldFold toroidal$fold;

    @Unique
    private @Nullable FoldedCompileContext toroidal$compileContext;

    @Unique
    private volatile @Nullable AquiferCells toroidal$aquiferCells;

    @ModifyArg(
            method = toroidal$CONSTRUCTOR,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/material/MaterialSystem;<init>("
                            + "Lnet/minecraft/world/level/levelgen/RandomState;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;I"
                            + "Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunction;"
                            + "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;)V"),
            index = 4)
    private PositionalRandomFactory toroidal$materialRandomFromCanonical(PositionalRandomFactory noiseRandom) {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        return fold == null ? noiseRandom : new CanonicalRandomFactory(noiseRandom, fold);
    }

    @ModifyArg(
            method = toroidal$CONSTRUCTOR,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunctionCompiler;<init>("
                            + "Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunction$CompileContext;)V"))
    private DensityFunction.CompileContext toroidal$compileWithTheFold(DensityFunction.CompileContext context,
            @Local(argsOnly = true) NoiseRouter router) {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        if (fold == null) {
            return context;
        }

        this.toroidal$compileContext = new FoldedCompileContext(context, fold, this,
                NoiseScaleLadder.of(fold, toroidal$roots(router), context));
        return this.toroidal$compileContext;
    }

    @Inject(method = toroidal$CONSTRUCTOR, at = @At("RETURN"))
    private void toroidal$runGenerationHooks(HolderGetter<NormalNoise> noises, long seed, boolean useLegacyRandom,
            BlockState defaultBlock, int seaLevel, NoiseRouter router, CallbackInfo callback) {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        this.toroidal$fold = fold;
        if (fold != null) {
            GenerationMoments.runAtRandomState((RandomState) (Object) this, new ToroidalShapeView(fold),
                    fold.generationOptions(), seaLevel);
        }
    }

    @Unique
    private static List<DensityFunction> toroidal$roots(NoiseRouter router) {
        return List.of(router.temperature(), router.vegetation(), router.continents(), router.erosion(),
                router.depth(), router.ridges(), router.chunkSurfaceLevel(), router.finalDensity());
    }

    @Override
    public @Nullable FoldedCompileContext toroidal$compileContext() {
        return this.toroidal$compileContext;
    }

    @Override
    public @Nullable AquiferCells toroidal$aquiferCells() {
        return this.toroidal$aquiferCells;
    }

    @Override
    public void toroidal$aquiferCells(AquiferCells cells) {
        this.toroidal$aquiferCells = cells;
    }

    @Override
    public @Nullable WorldFold toroidal$wrappedTransformer() {
        return this.toroidal$fold;
    }

    @Override
    public double toroidal$coastLift() {
        return this.toroidal$coastLift;
    }

    @Override
    public void toroidal$coastLift(double lift) {
        this.toroidal$coastLift = lift;
    }
}
