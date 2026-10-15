package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.exoticworlds.compat.create.CreateSeamFold;
import com.exoticworlds.compat.create.FoldedLinkable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public class RedstoneLinkNetworkHandlerMixin {
    @Unique
    private static final String SEATED_FROM = "toroidal$seatedFrom";

    @WrapOperation(method = "updateNetworkOf",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;withinRange("
                            + "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;"
                            + "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;)Z"))
    private boolean toroidal$foldLinkRange(IRedstoneLinkable from, IRedstoneLinkable to,
            Operation<Boolean> original, LevelAccessor world, @Share(SEATED_FROM) LocalRef<BlockPos> seatedFromRef) {
        if (from == to || !(world instanceof Level level)) {
            return original.call(from, to);
        }

        BlockPos storedFrom = from.getLocation();
        BlockPos storedTo = to.getLocation();
        BlockPos seatedFrom = seatedFromRef.get();
        if (seatedFrom == null) {
            seatedFrom = CreateSeamFold.worldSeat(level, storedFrom);
            seatedFromRef.set(seatedFrom);
        }

        BlockPos seatedTo = CreateSeamFold.worldSeat(level, storedTo);
        BlockPos foldedTo = CreateSeamFold.nearestCopy(level, seatedFrom, seatedTo);
        if (seatedFrom.equals(storedFrom) && foldedTo.equals(storedTo)) {
            return original.call(from, to);
        }

        return original.call(new FoldedLinkable(from, seatedFrom), new FoldedLinkable(to, foldedTo));
    }
}
