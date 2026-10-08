package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.phys.AABB;

@Mixin(BoatItem.class)
public class BoatItemMixin {
    @WrapOperation(
            method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_BOUNDING_BOX))
    private AABB toroidal$candidateBoxThroughSeam(Entity candidate, Operation<AABB> original,
            @Local(argsOnly = true) Player player) {
        AABB box = original.call(candidate);
        WorldFold transformer = ((TransformerSource) candidate).toroidal$wrappedTransformer();
        return FoldedBoxQuery.toward(transformer, player.getEyePosition(), box);
    }
}
