package com.exoticworlds.compat.lithostitched.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.lithostitched.CellLap;
import com.exoticworlds.compat.lithostitched.CellLapHolder;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import dev.worldgen.lithostitched.impl.worldgen.densityfunction.CellularDensityFunction;

@Mixin(CellularDensityFunction.XYZSampler.class)
public class CellularXYZSamplerMixin implements CellLapHolder {
    private static final String EXPRESSION = "MIXINEXTRAS:EXPRESSION";

    private static final int SEED_USE = 0;

    @Unique
    private @Nullable CellLap exotic_worlds$cellLap;

    @Override
    public @Nullable CellLap exotic_worlds$cellLap() {
        return this.exotic_worlds$cellLap;
    }

    @Override
    public void exotic_worlds$setCellLap(CellLap lap) {
        this.exotic_worlds$cellLap = lap;
    }

    @Definition(id = "spacedGridX", local = @Local(type = int.class, name = "spacedGridX"))
    @Definition(id = "xo", local = @Local(type = int.class, name = "xo"))
    @Expression("spacedGridX + xo")
    @ModifyExpressionValue(method = InjectionTargets.DENSITY_SAMPLER_SAMPLE_VALUE,
            at = @At(value = EXPRESSION, ordinal = SEED_USE))
    private int exotic_worlds$closedSeedX(int cellX, @Local(name = "spacedGridZ") int spacedGridZ,
            @Local(name = "zo") int zo) {
        CellLap lap = this.exotic_worlds$cellLap;
        return lap == null ? cellX : lap.foldX(cellX, spacedGridZ + zo);
    }

    @Definition(id = "spacedGridZ", local = @Local(type = int.class, name = "spacedGridZ"))
    @Definition(id = "zo", local = @Local(type = int.class, name = "zo"))
    @Expression("spacedGridZ + zo")
    @ModifyExpressionValue(method = InjectionTargets.DENSITY_SAMPLER_SAMPLE_VALUE,
            at = @At(value = EXPRESSION, ordinal = SEED_USE))
    private int exotic_worlds$closedSeedZ(int cellZ) {
        CellLap lap = this.exotic_worlds$cellLap;
        return lap == null ? cellZ : lap.foldZ(cellZ);
    }
}
