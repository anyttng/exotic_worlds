package com.exoticworlds.mixin;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.core.GenerationMoments;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.DensityRewrites;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.NoiseScaleLadder;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(RandomState.class)
public class RandomStateMixin {
    private static final String CONSTRUCTOR = "<init>(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;"
            + "Lnet/minecraft/core/HolderGetter;J)V";

    @ModifyExpressionValue(method = CONSTRUCTOR, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;noiseRouter()Lnet/minecraft/world/level/levelgen/NoiseRouter;"))
    private NoiseRouter toroidal$rewriteKeyedDensities(NoiseRouter router) {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        return fold != null ? DensityRewrites.apply(router, fold) : router;
    }

    // Before RETURN: C2ME compiles the router at its own RETURN inject and reads the separated scales there.
    @Inject(method = CONSTRUCTOR, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/levelgen/RandomState;router:Lnet/minecraft/world/level/levelgen/NoiseRouter;",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void toroidal$separateCloseNoiseScales(NoiseGeneratorSettings settings,
            HolderGetter<NormalNoise.NoiseParameters> noises, long seed, CallbackInfo callback) {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        if (fold != null) {
            NoiseScaleLadder.of(fold, toroidal$roots(((RandomState) (Object) this).router())).install();
        }
    }

    @Inject(method = CONSTRUCTOR, at = @At("RETURN"))
    private void toroidal$runGenerationHooks(NoiseGeneratorSettings settings,
            HolderGetter<NormalNoise.NoiseParameters> noises, long seed, CallbackInfo callback) {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        if (fold != null) {
            GenerationMoments.runAtRandomState((RandomState) (Object) this, new ToroidalShapeView(fold),
                    fold.generationOptions(), settings.seaLevel());
        }
    }

    @Unique
    private static List<DensityFunction> toroidal$roots(NoiseRouter router) {
        return List.of(router.temperature(), router.vegetation(), router.continents(), router.erosion(),
                router.depth(), router.ridges(), router.preliminarySurfaceLevel(), router.finalDensity());
    }
}
