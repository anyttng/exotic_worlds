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
    private long toroidal$seed;

    @Unique
    private volatile @Nullable SquareLapMap<WoverBiomePicker.PickableBiome> toroidal$lapMap;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$keepSeed(long seed, int size, WoverBiomePicker picker, CallbackInfo ci) {
        this.toroidal$seed = seed;
    }

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void toroidal$lapBiomeWithEdge(double x, double y, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer != null) {
            cir.setReturnValue(EdgeBiomes.square(toroidal$lapMap(transformer), x, z));
        }
    }

    @Inject(method = "getRawBiome", at = @At("HEAD"), cancellable = true)
    private void toroidal$lapBiome(double x, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer != null) {
            cir.setReturnValue(toroidal$lapMap(transformer).biomeAt(x, z));
        }
    }

    @Override
    public LapMap<WoverBiomePicker.PickableBiome> toroidal$lapMap(WorldFold fold) {
        SquareLapMap<WoverBiomePicker.PickableBiome> lapMap = this.toroidal$lapMap;
        if (lapMap == null || !lapMap.covers(fold)) {
            lapMap = new SquareLapMap<>(fold, this.sizeXZ, FoldCompression.of(fold), this.toroidal$seed,
                    new LapPicker<>(this.picker::getBiome, WoverBiomePicker.PickableBiome::getSubBiome));
            this.toroidal$lapMap = lapMap;
        }

        return lapMap;
    }
}
