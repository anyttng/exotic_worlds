package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.ultraterraforged.PoissonLattice;
import com.exoticworlds.compat.terraforged.RtfLap;

import com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement.poisson.FastPoisson;
import com.pandaismyname1.ultraterraforged.world.worldgen.feature.placement.poisson.FastPoissonContext;

@Mixin(value = FastPoisson.class, remap = false)
public abstract class FastPoissonMixin {
    @WrapMethod(method = "getPoint")
    private static long exotic_worlds$closeOnLap(int seed, float x, float z, FastPoissonContext context,
            Operation<Long> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(seed, x, z, context);
        }

        return PoissonLattice.point(frame, seed, x, z, context);
    }
}
