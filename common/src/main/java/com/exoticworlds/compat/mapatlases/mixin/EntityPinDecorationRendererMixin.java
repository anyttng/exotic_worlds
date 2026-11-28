package com.exoticworlds.compat.mapatlases.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.map.AtlasCenterSeat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.integration.moonlight.EntityPinDecorationRenderer;

@Mixin(EntityPinDecorationRenderer.class)
public class EntityPinDecorationRendererMixin {
    private static final String EXTRACT = "extract(Lpepjebs/mapatlases/integration/moonlight/EntityPinDecoration;"
            + "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;"
            + "Lnet/mehvahdjukaar/moonlight/api/map/client/MLDecorationRenderState;)Z";

    @ModifyExpressionValue(
            method = EXTRACT,
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_X,
                    opcode = Opcodes.GETFIELD))
    private int exotic_worlds$centerXNearEntity(int centerX, @Local(argsOnly = true) MapItemSavedData data,
            @Local Entity entity) {
        return AtlasCenterSeat.toward(Direction.Axis.X, data, entity.getX(), entity.getZ());
    }

    @ModifyExpressionValue(
            method = EXTRACT,
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_Z,
                    opcode = Opcodes.GETFIELD))
    private int exotic_worlds$centerZNearEntity(int centerZ, @Local(argsOnly = true) MapItemSavedData data,
            @Local Entity entity) {
        return AtlasCenterSeat.toward(Direction.Axis.Z, data, entity.getX(), entity.getZ());
    }
}
