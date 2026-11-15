package com.exoticworlds.compat.c2me.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.accessors.InterpretedDensity;
import com.ishland.c2me.opts.dfc.common.gen.jvm.AbstractCompiledDensityFunction;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(AbstractCompiledDensityFunction.class)
public abstract class CompiledDensityFallbackMixin implements InterpretedDensity {
    @Shadow
    protected abstract DensityFunction getFallback();

    @Override
    public @Nullable DensityFunction exotic_worlds$interpreted() {
        return this.getFallback();
    }
}
