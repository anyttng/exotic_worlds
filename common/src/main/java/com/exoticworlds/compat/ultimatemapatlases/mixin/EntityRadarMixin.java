package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.ultimatemapatlases.AtlasCenterSeat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.nanaky.ultimate_map_atlases.integration.moonlight.EntityRadar;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;

@Mixin(EntityRadar.class)
public class EntityRadarMixin {
    @ModifyExpressionValue(
            method = "renderMapMarkers",
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_X,
                    opcode = Opcodes.GETFIELD))
    private static int exotic_worlds$centerXNearEntity(int centerX, @Local(argsOnly = true) MapDataHolder holder,
            @Local Entity entity) {
        return AtlasCenterSeat.toward(Direction.Axis.X, holder.data, entity.getX(), entity.getZ());
    }

    @ModifyExpressionValue(
            method = "renderMapMarkers",
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_Z,
                    opcode = Opcodes.GETFIELD))
    private static int exotic_worlds$centerZNearEntity(int centerZ, @Local(argsOnly = true) MapDataHolder holder,
            @Local Entity entity) {
        return AtlasCenterSeat.toward(Direction.Axis.Z, holder.data, entity.getX(), entity.getZ());
    }
}
