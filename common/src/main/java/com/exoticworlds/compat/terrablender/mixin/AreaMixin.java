package com.exoticworlds.compat.terrablender.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.terrablender.LayeredArea;
import com.exoticworlds.compat.terrablender.RegionLayerStack;

import net.minecraft.core.Direction;

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
    private int toroidal$foldX(int x) {
        RegionLayerStack stack = this.toroidal$stack;
        return stack == null ? x : stack.fold(Direction.Axis.X, this.toroidal$depth, x);
    }

    @ModifyVariable(method = "get", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int toroidal$foldZ(int z) {
        RegionLayerStack stack = this.toroidal$stack;
        return stack == null ? z : stack.fold(Direction.Axis.Z, this.toroidal$depth, z);
    }
}
