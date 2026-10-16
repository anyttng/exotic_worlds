package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
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
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_DISTANCE_TO_SQR))
    private double toroidal$wrappedBlockReach(AABB block, Vec3 eye, Operation<Double> original) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(block, eye);
        }

        return transformer.sqrDistanceToBox(block, eye);
    }

    @WrapMethod(method = "isWithinEntityInteractionRange(Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean toroidal$wrappedEntityReach(AABB aabb, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        WorldFold transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        return original.call(FoldedBoxQuery.toward(transformer, player.getEyePosition(), aabb), buffer);
    }

    @WrapMethod(method = "isWithinAttackRange(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean toroidal$wrappedAttackReach(ItemStack weapon, AABB aabb, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        WorldFold transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        return original.call(weapon, FoldedBoxQuery.toward(transformer, player.getEyePosition(), aabb), buffer);
    }
}
