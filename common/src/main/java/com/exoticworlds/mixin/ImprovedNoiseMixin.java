package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;
import com.exoticworlds.engine.noise.PeriodicLattices;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

@Mixin(ImprovedNoise.class)
public class ImprovedNoiseMixin {
    @Shadow
    @Final
    private byte[] p;

    @Shadow
    @Final
    public double xo;

    @Shadow
    @Final
    public double yo;

    @Shadow
    @Final
    public double zo;

    @Unique
    private final PeriodicLattices toroidal$lattices = new PeriodicLattices();

    @WrapMethod(method = "noise(DDDDD)D")
    private double toroidal$periodicNoise(double x, double y, double z, double yScale, double yFudge, Operation<Double> original) {
        Context context = GenerationTransformerContext.context();
        WorldFold transformer = context.wrappedTransformer();
        if (transformer == null) {
            return original.call(x, y, z, yScale, yFudge);
        }

        return this.toroidal$lattices.sample(this.p, this.xo, this.yo, this.zo, transformer,
                context, x, y, z, yScale, yFudge);
    }
}
