package com.exoticworlds.compat.wover.mixin;

import java.util.ArrayList;
import java.util.List;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker;
import org.betterx.wover.generator.api.map.BiomeMap;
import org.betterx.wover.generator.api.map.MapBuilderFunction;
import org.betterx.wover.generator.impl.map.MapStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.compat.wover.LapMap;
import com.exoticworlds.compat.wover.LapMapHolder;
import com.exoticworlds.compat.wover.LapMapStack;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

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
    private long toroidal$seed;

    @Unique
    private volatile @Nullable LapMapStack<WoverBiomePicker.PickableBiome> toroidal$lapStack;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$keepSeed(long seed, int size, WoverBiomePicker picker, int mapHeight, int worldHeight,
            MapBuilderFunction mapConstructor, CallbackInfo ci) {
        this.toroidal$seed = seed;
    }

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void toroidal$lapLayer(double x, double y, double z,
            CallbackInfoReturnable<WoverBiomePicker.PickableBiome> cir) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer != null) {
            cir.setReturnValue(this.maps[toroidal$lapStack(transformer).layer(x, y, z)].getBiome(x, y, z));
        }
    }

    @Unique
    private LapMapStack<WoverBiomePicker.PickableBiome> toroidal$lapStack(WorldFold fold) {
        LapMapStack<WoverBiomePicker.PickableBiome> stack = this.toroidal$lapStack;
        if (stack != null && stack.covers(fold)) {
            return stack;
        }

        synchronized (this) {
            stack = this.toroidal$lapStack;
            if (stack == null || !stack.covers(fold)) {
                stack = new LapMapStack<>(fold, toroidal$layers(fold), biome -> biome.isVertical,
                        new LapMapStack.Layering(this.minValue, this.maxValue, this.maxIndex, this.worldHeight,
                                this.layerDistortion),
                        this.toroidal$seed);
                this.toroidal$lapStack = stack;
            }

            return stack;
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private List<LapMap<WoverBiomePicker.PickableBiome>> toroidal$layers(WorldFold fold) {
        List<LapMap<WoverBiomePicker.PickableBiome>> layers = new ArrayList<>(this.maps.length);
        for (BiomeMap map : this.maps) {
            if (!(map instanceof LapMapHolder<?> holder)) {
                return List.of();
            }

            layers.add(((LapMapHolder<WoverBiomePicker.PickableBiome>) holder).toroidal$lapMap(fold));
        }

        return layers;
    }
}
