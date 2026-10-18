package com.exoticworlds.compat.terrablender.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.exoticworlds.compat.terrablender.LayeredArea;
import com.exoticworlds.compat.terrablender.RegionLayerFold;
import com.exoticworlds.compat.terrablender.RegionLayerStack;

import terrablender.worldgen.noise.Area;

@Mixin(Area.class)
public class AreaMixin implements LayeredArea {
    @Unique
    private @Nullable RegionLayerStack exotic_worlds$stack;

    @Unique
    private int exotic_worlds$depth;

    @Override
    public void exotic_worlds$register(RegionLayerStack stack, int depth) {
        this.exotic_worlds$stack = stack;
        this.exotic_worlds$depth = depth;
        stack.raise(depth);
    }

    @Override
    public @Nullable RegionLayerStack exotic_worlds$stack() {
        return this.exotic_worlds$stack;
    }

    @Override
    public int exotic_worlds$depth() {
        return this.exotic_worlds$depth;
    }

    @ModifyVariable(method = "get", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int exotic_worlds$foldCoordinates(int x, @Local(argsOnly = true, ordinal = 1) LocalIntRef z) {
        RegionLayerStack stack = this.exotic_worlds$stack;
        RegionLayerFold fold = stack == null ? null : stack.resolved();
        if (fold == null) {
            return x;
        }

        int rawZ = z.get();
        z.set(fold.foldZ(this.exotic_worlds$depth, rawZ));
        return fold.foldX(this.exotic_worlds$depth, x, rawZ);
    }
}
