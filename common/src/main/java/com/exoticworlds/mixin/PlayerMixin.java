package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(Player.class)
public class PlayerMixin {
    @WrapOperation(
            method = "isWithinBlockInteractionRange",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private double exotic_worlds$wrappedBlockReach(AABB block, Vec3 eye, Operation<Double> original) {
        WorldFold transformer = ((TransformerSource) this).exotic_worlds$wrappedTransformer();
        if (transformer == null) {
            return original.call(block, eye);
        }

        return transformer.sqrDistanceToBox(block, eye);
    }

    @WrapMethod(method = "isWithinEntityInteractionRange(Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean exotic_worlds$wrappedEntityReach(AABB aabb, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        return original.call(FoldedBoxQuery.toward(player, player.getEyePosition(), aabb), buffer);
    }

    @WrapMethod(method = "isWithinAttackRange(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean exotic_worlds$wrappedAttackReach(ItemStack weapon, AABB aabb, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        return original.call(weapon, FoldedBoxQuery.toward(player, player.getEyePosition(), aabb), buffer);
    }
}
