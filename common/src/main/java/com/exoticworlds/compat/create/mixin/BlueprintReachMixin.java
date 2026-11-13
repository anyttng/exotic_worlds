package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.zurrtum.create.content.equipment.blueprint.BlueprintEntity;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@Mixin(BlueprintEntity.class)
public class BlueprintReachMixin {
    @ModifyVariable(method = "canPlayerUse", at = @At("STORE"), ordinal = 0)
    private AABB exotic_worlds$foldBlueprintBox(AABB box, Player player) {
        return FoldedBoxQuery.toward(
                WorldLoopAttachments.wrappedFoldOfReader(player.level()), player.position(), box);
    }
}
