package com.exoticworlds.compat.terrablender.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.terrablender.LayeredArea;
import com.exoticworlds.compat.terrablender.RegionLayerStack;

import terrablender.worldgen.noise.Area;

@Mixin(value = Area.class, remap = false)
public class AreaMixin implements LayeredArea {
    @Unique
    private @Nullable RegionLayerStack toroidal$stack;

    @Unique
    private int toroidal$depth;

    @Override
    public void toroidal$enrol(RegionLayerStack stack, int depth) {
        this.toroidal$stack = stack;
        this.toroidal$depth = depth;
        stack.raise(depth);
    }

    @Override
    public @Nullable RegionLayerStack toroidal$stack() {
        return this.toroidal$stack;
    }

    @Override
    public int toroidal$depth() {
        return this.toroidal$depth;
    }

    @ModifyVariable(method = "get", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int toroidal$foldX(int x, @Local(argsOnly = true, ordinal = 1) int z) {
        RegionLayerStack stack = this.toroidal$stack;
        return stack == null ? x : stack.foldX(this.toroidal$depth, x, z);
    }

    @ModifyVariable(method = "get", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int toroidal$foldZ(int z) {
        RegionLayerStack stack = this.toroidal$stack;
        return stack == null ? z : stack.foldZ(this.toroidal$depth, z);
    }
}
