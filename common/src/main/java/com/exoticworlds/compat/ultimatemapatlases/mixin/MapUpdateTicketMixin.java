package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.ultimatemapatlases.AtlasCenterSeat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;

@Mixin(targets = "net.nanaky.ultimate_map_atlases.lifecycle.MapAtlasesServerEvents$MapUpdateTicket")
public class MapUpdateTicketMixin {
    @Shadow
    @Final
    private MapDataHolder holder;

    @ModifyExpressionValue(
            method = "updatePriority",
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_X,
                    opcode = Opcodes.GETFIELD))
    private int exotic_worlds$centerXNearPlayer(int centerX, @Local(argsOnly = true, ordinal = 0) int px,
            @Local(argsOnly = true, ordinal = 1) int pz) {
        return AtlasCenterSeat.toward(Direction.Axis.X, this.holder.data, px, pz);
    }

    @ModifyExpressionValue(
            method = "updatePriority",
            at = @At(value = "FIELD", target = InjectionTargets.MAP_ITEM_SAVED_DATA_CENTER_Z,
                    opcode = Opcodes.GETFIELD))
    private int exotic_worlds$centerZNearPlayer(int centerZ, @Local(argsOnly = true, ordinal = 0) int px,
            @Local(argsOnly = true, ordinal = 1) int pz) {
        return AtlasCenterSeat.toward(Direction.Axis.Z, this.holder.data, px, pz);
    }
}
