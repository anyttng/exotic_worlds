package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.ultimatemapatlases.AtlasCenterSeat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.nanaky.ultimate_map_atlases.map_collection.MapCollection;

@Mixin(MapCollection.class)
public class MapCollectionMixin {
    @ModifyExpressionValue(
            method = "distSquare",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;centerX:I",
                    opcode = Opcodes.GETFIELD))
    private static int exotic_worlds$centerXNearPoint(int centerX, @Local(argsOnly = true) MapItemSavedData mapState,
            @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double z) {
        return AtlasCenterSeat.toward(Direction.Axis.X, mapState, x, z);
    }

    @ModifyExpressionValue(
            method = "distSquare",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;centerZ:I",
                    opcode = Opcodes.GETFIELD))
    private static int exotic_worlds$centerZNearPoint(int centerZ, @Local(argsOnly = true) MapItemSavedData mapState,
            @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double z) {
        return AtlasCenterSeat.toward(Direction.Axis.Z, mapState, x, z);
    }
}
