package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.exoticworlds.compat.cobblemon.BattleSeat;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.phys.Vec3;

@Mixin(ActiveBattlePokemon.class)
public class SendOutPositionMixin {
    private static final String SEND_OUT_POSITION = "getSendOutPosition()Lnet/minecraft/world/phys/Vec3;";

    @WrapOperation(method = SEND_OUT_POSITION, at = @At(value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/api/battles/model/actor/EntityBackedBattleActor;getInitialPos()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$seatInitialPos(EntityBackedBattleActor<?> actor, Operation<Vec3> original) {
        return BattleSeat.seated((ActiveBattlePokemon) (Object) this, original.call(actor));
    }

    @ModifyReturnValue(method = SEND_OUT_POSITION, at = @At("RETURN"))
    private Vec3 exotic_worlds$canonicalSendOut(Vec3 sendOut) {
        return BattleSeat.canonical((ActiveBattlePokemon) (Object) this, sendOut);
    }
}
