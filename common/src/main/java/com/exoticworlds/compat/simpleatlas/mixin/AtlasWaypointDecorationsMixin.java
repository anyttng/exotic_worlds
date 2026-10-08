package com.exoticworlds.compat.simpleatlas.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.MapSeamFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import rubbertoe.simple_atlas.component.AtlasContents;
import rubbertoe.simple_atlas.server.AtlasWaypointDecorations;

@Mixin(AtlasWaypointDecorations.class)
public class AtlasWaypointDecorationsMixin {
    @ModifyExpressionValue(
            method = "toDecoration",
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_X,
                    opcode = Opcodes.GETFIELD))
    private static int toroidal$centerXNearWaypoint(int centerX, @Local(argsOnly = true) MapItemSavedData data,
            @Local(argsOnly = true) AtlasContents.WaypointData waypoint) {
        return (int) NearestCopy.toward(MapSeamFold.transformerFor(null, data.dimension), Direction.Axis.X,
                waypoint.worldX(), centerX);
    }

    @ModifyExpressionValue(
            method = "toDecoration",
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_Z,
                    opcode = Opcodes.GETFIELD))
    private static int toroidal$centerZNearWaypoint(int centerZ, @Local(argsOnly = true) MapItemSavedData data,
            @Local(argsOnly = true) AtlasContents.WaypointData waypoint) {
        return (int) NearestCopy.toward(MapSeamFold.transformerFor(null, data.dimension), Direction.Axis.Z,
                waypoint.worldZ(), centerZ);
    }
}
