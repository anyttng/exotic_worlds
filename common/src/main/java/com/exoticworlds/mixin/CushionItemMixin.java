package com.exoticworlds.mixin;

import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.item.CushionItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

@Mixin(CushionItem.class)
public class CushionItemMixin {
    @ModifyExpressionValue(
            method = "recalculateContextForSpecialCollisionShapes",
            at = @At(value = "INVOKE", target = InjectionTargets.PLAYER_GET_EYE_POSITION))
    private static Vec3 exotic_worlds$eyeBesideClick(Vec3 eyePosition, @Local(argsOnly = true) UseOnContext context) {
        FoldSource player = (FoldSource) Objects.requireNonNull(context.getPlayer());
        return NearestCopy.toward(player.exotic_worlds$wrappedFold(), context.getClickLocation(), eyePosition);
    }
}
