package com.exoticworlds.compat.wover.ambertation.mixin;

import de.ambertation.wover.generator.api.biomesource.WoverBiomePicker;
import de.ambertation.wover.generator.impl.map.square.SquareBiomeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.compat.worldgen.FoldCompression;
import com.exoticworlds.compat.wover.EdgeBiomes;
import com.exoticworlds.compat.wover.LapMap;
import com.exoticworlds.compat.wover.LapMapHolder;
import com.exoticworlds.compat.wover.LapMapMemo;
import com.exoticworlds.compat.wover.SquareLapMap;
import com.exoticworlds.compat.wover.ambertation.PickableBiomeTraits;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;

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
    private final LapMapMemo<WoverBiomePicker.PickableBiome> exotic_worlds$lapMaps = new LapMapMemo<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$keepSeed(long seed, int size, WoverBiomePicker picker, CallbackInfo ci) {
        this.exotic_worlds$seed = seed;
    }

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$lapBiomeWithEdge(double x, double y, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        if (fold != null) {
            cir.setReturnValue(EdgeBiomes.square(exotic_worlds$lapMap(fold), PickableBiomeTraits.INSTANCE, x, z));
        }
    }

    @Inject(method = "getRawBiome", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$lapBiome(double x, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        if (fold != null) {
            cir.setReturnValue(exotic_worlds$lapMap(fold).biomeAt(x, z));
        }
    }

    @Override
    public LapMap<WoverBiomePicker.PickableBiome> exotic_worlds$lapMap(WorldFold fold) {
        LapMap<WoverBiomePicker.PickableBiome> lapMap = this.exotic_worlds$lapMaps.getCovering(fold);
        return lapMap != null ? lapMap : this.exotic_worlds$lapMaps.keep(new SquareLapMap<>(fold, this.sizeXZ,
                FoldCompression.of(fold), this.exotic_worlds$seed, PickableBiomeTraits.toLapPicker(this.picker)));
    }
}
