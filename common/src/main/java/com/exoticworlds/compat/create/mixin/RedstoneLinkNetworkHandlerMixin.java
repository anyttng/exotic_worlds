package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.redstone.link.IRedstoneLinkable;
import com.zurrtum.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.exoticworlds.compat.create.CreateSeamFold;
import com.exoticworlds.compat.create.FoldedLinkable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

@Mixin(RedstoneLinkNetworkHandler.class)
public class RedstoneLinkNetworkHandlerMixin {
    @WrapOperation(method = "updateNetworkOf",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/content/redstone/link/RedstoneLinkNetworkHandler;withinRange("
                            + "Lcom/zurrtum/create/content/redstone/link/IRedstoneLinkable;"
                            + "Lcom/zurrtum/create/content/redstone/link/IRedstoneLinkable;)Z"))
    private boolean exotic_worlds$foldLinkRange(IRedstoneLinkable from, IRedstoneLinkable to,
            Operation<Boolean> original, LevelAccessor world) {
        if (from == to || !(world instanceof Level level)) {
            return original.call(from, to);
        }

        BlockPos storedTo = to.getLocation();
        BlockPos foldedTo = CreateSeamFold.nearestCopy(level, from.getLocation(), storedTo);
        if (foldedTo.equals(storedTo)) {
            return original.call(from, to);
        }

        return original.call(from, new FoldedLinkable(to, foldedTo));
    }
}
