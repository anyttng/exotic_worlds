package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.item.CinderousSoulcallerItem;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = CinderousSoulcallerItem.class, remap = false)
public abstract class CinderousSoulcallerItemMixin {
    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/util/Utils;getAngle(DDDD)F"))
    private float exotic_worlds$bossFacesNearestPlayer(double bossX, double bossZ, double playerX, double playerZ,
            Operation<Float> original, @Local(argsOnly = true) Level level) {
        Vec3 player = NearestCopy.toward(WorldLoopAttachments.wrappedFoldOf(level), new Vec3(bossX, 0.0, bossZ),
                new Vec3(playerX, 0.0, playerZ));
        return original.call(bossX, bossZ, player.x, player.z);
    }
}
