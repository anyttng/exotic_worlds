package com.exoticworlds.compat.simpleatlas.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import rubbertoe.simple_atlas.map.AtlasMapSelector;

@Mixin(AtlasMapSelector.class)
public class AtlasMapSelectorMixin {
    @ModifyExpressionValue(
            method = {"findCurrentMapRawId", "mapContainsPosition"},
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_X,
                    opcode = Opcodes.GETFIELD))
    private static int toroidal$centerXNearPosition(int centerX, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true, ordinal = 0) double x) {
        return (int) NearestCopy.toward(WorldLoopAttachments.wrappedTransformerOf(level), Direction.Axis.X, x,
                centerX);
    }

    @ModifyExpressionValue(
            method = {"findCurrentMapRawId", "mapContainsPosition"},
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_Z,
                    opcode = Opcodes.GETFIELD))
    private static int toroidal$centerZNearPosition(int centerZ, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true, ordinal = 1) double z) {
        return (int) NearestCopy.toward(WorldLoopAttachments.wrappedTransformerOf(level), Direction.Axis.Z, z,
                centerZ);
    }
}
