package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.simibubi.create.content.equipment.blueprint.BlueprintEntity;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@Mixin(value = BlueprintEntity.class, remap = false)
public class BlueprintReachMixin {
    @ModifyVariable(method = "canPlayerUse", at = @At("STORE"), ordinal = 0)
    private AABB exotic_worlds$foldBlueprintBox(AABB box, Player player) {
        return FoldedBoxQuery.toward(
                WorldLoopAttachments.wrappedFoldOfReader(player.level()), player.position(), box);
    }
}
