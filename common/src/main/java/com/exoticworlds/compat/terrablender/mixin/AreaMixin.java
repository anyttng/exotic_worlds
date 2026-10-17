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
    private int toroidal$foldCoordinates(int x, @Local(argsOnly = true, ordinal = 1) LocalIntRef z) {
        RegionLayerStack stack = this.toroidal$stack;
        RegionLayerFold fold = stack == null ? null : stack.resolved();
        if (fold == null) {
            return x;
        }

        int rawZ = z.get();
        z.set(fold.foldZ(this.toroidal$depth, rawZ));
        return fold.foldX(this.toroidal$depth, x, rawZ);
    }
}
