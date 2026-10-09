package com.exoticworlds.compat.wover.mixin;

import org.betterx.wover.math.api.MathHelper;
import org.betterx.wover.surface.impl.conditions.ThresholdConditionImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.wover.LapNoise;
import com.exoticworlds.compat.wover.OpenSimplexStandIn;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;

@Mixin(ThresholdConditionImpl.class)
public class ThresholdConditionImplMixin {
    private static final int ROUGHNESS_Y = 0;

    @Shadow
    @Final
    private double threshold;

    @Shadow
    @Final
    private FloatProvider roughness;

    @Shadow
    @Final
    private double scaleX;

    @Shadow
    @Final
    private double scaleZ;

    @Unique
    private long toroidal$seed;

    @Unique
    private OpenSimplexStandIn toroidal$standIn;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$buildStandIn(long noiseSeed, double threshold, FloatProvider roughness, double scaleX,
            double scaleZ, CallbackInfo ci) {
        this.toroidal$seed = noiseSeed;
        this.toroidal$standIn = new OpenSimplexStandIn(noiseSeed);
    }

    @WrapMethod(method = "test")
    private boolean toroidal$lapTest(MaterialRuleContext context, Operation<Boolean> original) {
        WorldFold fold = GenerationTransformerContext.context().wrappedTransformer();
        if (fold == null) {
            return original.call(context);
        }

        int x = context.blockX();
        int z = context.blockZ();
        double noise = LapNoise.eval(this.toroidal$standIn, fold, x * this.scaleX, z * this.scaleZ, this.scaleX,
                this.scaleZ);
        RandomSource random = RandomSource.create(
                MathHelper.getSeed(Long.hashCode(this.toroidal$seed), x, ROUGHNESS_Y, z));
        return noise + this.roughness.sample(random) > this.threshold;
    }
}
