package com.exoticworlds.compat.wover.mixin;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker;
import org.betterx.wover.generator.impl.map.square.SquareBiomeMap;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.compat.FoldCompression;
import com.exoticworlds.compat.wover.EdgeBiomes;
import com.exoticworlds.compat.wover.LapMap;
import com.exoticworlds.compat.wover.LapMapHolder;
import com.exoticworlds.compat.wover.LapPicker;
import com.exoticworlds.compat.wover.SquareLapMap;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

@Mixin(SquareBiomeMap.class)
public class SquareBiomeMapMixin implements LapMapHolder<WoverBiomePicker.PickableBiome> {
    @Shadow
    @Final
    private WoverBiomePicker picker;

    @Shadow
    @Final
    private int sizeXZ;

    @Unique
    private long exotic_worlds$seed;

    @Unique
    private volatile @Nullable SquareLapMap<WoverBiomePicker.PickableBiome> exotic_worlds$lapMap;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$keepSeed(long seed, int size, WoverBiomePicker picker, CallbackInfo ci) {
        this.exotic_worlds$seed = seed;
    }

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$lapBiomeWithEdge(double x, double y, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer != null) {
            cir.setReturnValue(EdgeBiomes.square(exotic_worlds$lapMap(transformer), x, z));
        }
    }

    @Inject(method = "getRawBiome", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$lapBiome(double x, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer != null) {
            cir.setReturnValue(exotic_worlds$lapMap(transformer).biomeAt(x, z));
        }
    }

    @Override
    public LapMap<WoverBiomePicker.PickableBiome> exotic_worlds$lapMap(WorldFold fold) {
        SquareLapMap<WoverBiomePicker.PickableBiome> lapMap = this.exotic_worlds$lapMap;
        if (lapMap == null || !lapMap.covers(fold)) {
            lapMap = new SquareLapMap<>(fold, this.sizeXZ, FoldCompression.of(fold), this.exotic_worlds$seed,
                    new LapPicker<>(this.picker::getBiome, WoverBiomePicker.PickableBiome::getSubBiome));
            this.exotic_worlds$lapMap = lapMap;
        }

        return lapMap;
    }
}
