package com.exoticworlds.compat.wover.betterx.mixin;

import java.util.List;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker;
import org.betterx.wover.generator.api.map.BiomeMap;
import org.betterx.wover.generator.api.map.MapBuilderFunction;
import org.betterx.wover.generator.impl.map.MapStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.wover.LapMap;
import com.exoticworlds.compat.wover.LapMapStack;
import com.exoticworlds.compat.wover.LapMapStackMemo;
import com.exoticworlds.compat.wover.StackLayers;
import com.exoticworlds.compat.wover.betterx.PickableBiomeTraits;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;

@Mixin(MapStack.class)
public class MapStackMixin {
    @Shadow
    @Final
    private BiomeMap[] maps;

    @Shadow
    @Final
    private double layerDistortion;

    @Shadow
    @Final
    private int worldHeight;

    @Shadow
    @Final
    private int minValue;

    @Shadow
    @Final
    private int maxValue;

    @Shadow
    @Final
    private int maxIndex;

    @Unique
    private static final Logger exotic_worlds$LOGGER = LogUtils.getLogger();

    @Unique
    private final StackLayers<WoverBiomePicker.PickableBiome> exotic_worlds$stackLayers = new StackLayers<>(
            map -> exotic_worlds$LOGGER.warn("[ew/wover-compat] stack_unclosed map={}", map.getName()));

    @Unique
    private long exotic_worlds$seed;

    @Unique
    private final LapMapStackMemo<WoverBiomePicker.PickableBiome> exotic_worlds$lapStacks = new LapMapStackMemo<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$keepSeed(long seed, int size, WoverBiomePicker picker, int mapHeight, int worldHeight,
            MapBuilderFunction mapConstructor, CallbackInfo ci) {
        this.exotic_worlds$seed = seed;
    }

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$lapLayer(double x, double y, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        if (fold != null) {
            cir.setReturnValue(this.maps[exotic_worlds$lapStack(fold).layer(x, y, z)].getBiome(x, y, z));
        }
    }

    @Unique
    private LapMapStack<WoverBiomePicker.PickableBiome> exotic_worlds$lapStack(WorldFold fold) {
        return this.exotic_worlds$lapStacks.ensureCovering(fold, this::exotic_worlds$buildLapStack);
    }

    @Unique
    private LapMapStack<WoverBiomePicker.PickableBiome> exotic_worlds$buildLapStack(WorldFold fold) {
        return new LapMapStack<>(fold, exotic_worlds$layers(fold), PickableBiomeTraits.INSTANCE::isVertical,
                new LapMapStack.Layering(this.minValue, this.maxValue, this.maxIndex, this.worldHeight,
                        this.layerDistortion),
                this.exotic_worlds$seed);
    }

    @Unique
    private List<LapMap<WoverBiomePicker.PickableBiome>> exotic_worlds$layers(WorldFold fold) {
        return this.exotic_worlds$stackLayers.of(this.maps, fold);
    }
}
