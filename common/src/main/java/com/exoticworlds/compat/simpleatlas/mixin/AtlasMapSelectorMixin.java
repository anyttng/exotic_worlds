package com.exoticworlds.compat.simpleatlas.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.simpleatlas.MapCentreSeat;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import rubbertoe.simple_atlas.map.AtlasMapSelector;

@Mixin(AtlasMapSelector.class)
public class AtlasMapSelectorMixin {
    @ModifyExpressionValue(
            method = {"findCurrentMapRawId", "mapContainsPosition"},
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_X,
                    opcode = Opcodes.GETFIELD))
    private static int toroidal$centerXNearPosition(int centerX, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double z,
            @Local(name = "mapData") MapItemSavedData mapData) {
        return MapCentreSeat.toward(WorldLoopAttachments.wrappedTransformerOf(level), Direction.Axis.X, x, z, mapData);
    }

    @ModifyExpressionValue(
            method = {"findCurrentMapRawId", "mapContainsPosition"},
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_Z,
                    opcode = Opcodes.GETFIELD))
    private static int toroidal$centerZNearPosition(int centerZ, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double z,
            @Local(name = "mapData") MapItemSavedData mapData) {
        return MapCentreSeat.toward(WorldLoopAttachments.wrappedTransformerOf(level), Direction.Axis.Z, x, z, mapData);
    }
}
