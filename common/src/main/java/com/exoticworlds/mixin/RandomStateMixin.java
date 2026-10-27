package com.exoticworlds.mixin;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.accessors.FoldedClimateSampler;
import com.exoticworlds.core.GenerationMoments;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.gen.CanonicalRandomFactory;
import com.exoticworlds.engine.noise.DensityRewrites;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.NoiseScaleLadder;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(RandomState.class)
public class RandomStateMixin {
    private static final String CONSTRUCTOR = "<init>(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;"
            + "Lnet/minecraft/core/HolderGetter;J)V";

    @ModifyExpressionValue(method = CONSTRUCTOR, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;noiseRouter()Lnet/minecraft/world/level/levelgen/NoiseRouter;"))
    private NoiseRouter exotic_worlds$rewriteKeyedDensities(NoiseRouter router) {
        WorldFold fold = GenerationFoldContext.context().routerBuildFold();
        return fold != null ? DensityRewrites.apply(router, fold) : router;
    }

    @ModifyArg(
            method = CONSTRUCTOR,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/SurfaceSystem;<init>("
                            + "Lnet/minecraft/world/level/levelgen/RandomState;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;I"
                            + "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;)V"),
            index = 3)
    private PositionalRandomFactory exotic_worlds$surfaceRandomFromCanonical(PositionalRandomFactory noiseRandom) {
        return exotic_worlds$canonical(noiseRandom);
    }

    @ModifyExpressionValue(
            method = CONSTRUCTOR,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;forkPositional()"
                            + "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;"),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=ore"),
                    to = @At(value = "FIELD",
                            target = "Lnet/minecraft/world/level/levelgen/RandomState;oreRandom:"
                                    + "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;",
                            opcode = Opcodes.PUTFIELD)))
    private PositionalRandomFactory exotic_worlds$oreRandomFromCanonical(PositionalRandomFactory oreRandom) {
        return exotic_worlds$canonical(oreRandom);
    }

    // Before RETURN: C2ME compiles the router at its own RETURN inject and reads the separated scales there.
    @Inject(method = CONSTRUCTOR, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/levelgen/RandomState;router:Lnet/minecraft/world/level/levelgen/NoiseRouter;",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void exotic_worlds$separateCloseNoiseScales(NoiseGeneratorSettings settings,
            HolderGetter<NormalNoise.NoiseParameters> noises, long seed, CallbackInfo callback) {
        WorldFold fold = GenerationFoldContext.context().routerBuildFold();
        if (fold != null) {
            NoiseScaleLadder.of(fold, exotic_worlds$roots(((RandomState) (Object) this).router())).install();
        }
    }

    @Inject(method = CONSTRUCTOR, at = @At("RETURN"))
    private void exotic_worlds$runGenerationHooks(NoiseGeneratorSettings settings,
            HolderGetter<NormalNoise.NoiseParameters> noises, long seed, CallbackInfo callback) {
        WorldFold fold = GenerationFoldContext.context().routerBuildFold();
        if (fold != null) {
            ((FoldedClimateSampler) (Object) ((RandomState) (Object) this).sampler()).exotic_worlds$fold(fold);
            GenerationMoments.runAtRandomState((RandomState) (Object) this, new ToroidalShapeView(fold),
                    fold.generationOptions(), settings.seaLevel());
        }
    }

    @Unique
    private static PositionalRandomFactory exotic_worlds$canonical(PositionalRandomFactory factory) {
        WorldFold fold = GenerationFoldContext.context().routerBuildFold();
        return fold == null ? factory : new CanonicalRandomFactory(factory, fold);
    }

    @Unique
    private static List<DensityFunction> exotic_worlds$roots(NoiseRouter router) {
        return List.of(router.temperature(), router.vegetation(), router.continents(), router.erosion(),
                router.depth(), router.ridges(), router.initialDensityWithoutJaggedness(), router.finalDensity());
    }
}
