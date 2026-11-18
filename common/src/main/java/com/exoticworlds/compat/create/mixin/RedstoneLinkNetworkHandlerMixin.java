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
import com.exoticworlds.core.DeckTransformation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public class RedstoneLinkNetworkHandlerMixin {
    @Unique
    private static final String SEATED_FROM = "exotic_worlds$seatedFrom";

    @WrapOperation(method = "updateNetworkOf",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;withinRange("
                            + "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;"
                            + "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;)Z"))
    private boolean exotic_worlds$foldLinkRange(IRedstoneLinkable from, IRedstoneLinkable to,
            Operation<Boolean> original, LevelAccessor world, @Share(SEATED_FROM) LocalRef<Vec3> seatedFromRef) {
        if (from == to || !(world instanceof Level level)) {
            return original.call(from, to);
        }

        BlockPos storedFrom = from.getLocation();
        Vec3 seatedFrom = seatedFromRef.get();
        if (seatedFrom == null) {
            seatedFrom = CreateSeamFold.worldSeat(level, storedFrom);
            seatedFromRef.set(seatedFrom);
        }

        BlockPos storedTo = to.getLocation();
        Vec3 seatedTo = CreateSeamFold.worldSeat(level, storedTo);
        DeckTransformation lap = CreateSeamFold.nearestCopyTransformation(level, seatedFrom, seatedTo);
        if (lap.isIdentity()) {
            return original.call(from, to);
        }

        if (seatedTo.equals(Vec3.atCenterOf(storedTo))) {
            return original.call(from, new FoldedLinkable(to, lap.apply(storedTo)));
        }

        if (seatedFrom.equals(Vec3.atCenterOf(storedFrom))) {
            return original.call(new FoldedLinkable(from, lap.inverse().apply(storedFrom)), to);
        }

        return original.call(from, new FoldedLinkable(to, BlockPos.containing(lap.apply(seatedTo))));
    }
}
