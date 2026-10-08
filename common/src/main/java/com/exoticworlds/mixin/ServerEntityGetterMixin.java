package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerEntityGetter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerEntityGetter.class)
public interface ServerEntityGetterMixin {
    @WrapOperation(
            method = "getNearbyPlayers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;contains(DDD)Z"))
    private static boolean toroidal$nearbyPlayerThroughSeam(AABB box, double x, double y, double z,
            Operation<Boolean> original, @Local Player player) {
        return original.call(FoldedBoxQuery.toward(((TransformerSource) player).toroidal$wrappedTransformer(),
                new Vec3(x, y, z), box), x, y, z);
    }
}
