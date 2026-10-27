package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@Mixin(Player.class)
public class PlayerMixin {
    @WrapMethod(method = "canInteractWithBlock")
    private boolean exotic_worlds$wrappedBlockReach(BlockPos pos, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        WorldFold fold = ((FoldSource) player).exotic_worlds$wrappedFold();
        if (fold == null) {
            return original.call(pos, buffer);
        }

        return original.call(fold.nearestCopy(player.blockPosition(), pos), buffer);
    }

    @WrapMethod(method = "canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean exotic_worlds$wrappedEntityReach(AABB aabb, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        return original.call(FoldedBoxQuery.toward(player, player.getEyePosition(), aabb), buffer);
    }
}
