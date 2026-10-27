package com.exoticworlds.compat.wover.mixin;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker;
import org.betterx.wover.generator.impl.map.hex.HexBiomeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.compat.FoldCompression;
import com.exoticworlds.compat.wover.EdgeBiomes;
import com.exoticworlds.compat.wover.HexLapMap;
import com.exoticworlds.compat.wover.LapMap;
import com.exoticworlds.compat.wover.LapMapHolder;
import com.exoticworlds.compat.wover.LapMapMemo;
import com.exoticworlds.compat.wover.LapPicker;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;

@Mixin(HexBiomeMap.class)
public class HexBiomeMapMixin implements LapMapHolder<WoverBiomePicker.PickableBiome> {
    @Shadow
    @Final
    private WoverBiomePicker picker;

    @Shadow
    @Final
    private float scale;

    @Shadow
    @Final
    private int seed;

    @Unique
    private final LapMapMemo<WoverBiomePicker.PickableBiome> exotic_worlds$lapMaps = new LapMapMemo<>();

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$lapBiomeWithEdge(double x, double y, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold fold = GenerationFoldContext.context().wrappedFold();
        if (fold != null) {
            cir.setReturnValue(EdgeBiomes.hex(exotic_worlds$lapMap(fold), x, z));
        }
    }

    @Inject(method = "getRawBiome", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$lapBiome(double x, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold fold = GenerationFoldContext.context().wrappedFold();
        if (fold != null) {
            cir.setReturnValue(exotic_worlds$lapMap(fold).biomeAt(x, z));
        }
    }

    @Override
    public LapMap<WoverBiomePicker.PickableBiome> exotic_worlds$lapMap(WorldFold fold) {
        LapMap<WoverBiomePicker.PickableBiome> lapMap = this.exotic_worlds$lapMaps.covering(fold);
        return lapMap != null ? lapMap : this.exotic_worlds$lapMaps.keep(
                new HexLapMap<>(fold, this.scale, FoldCompression.of(fold), this.seed, LapPicker.of(this.picker)));
    }
}
