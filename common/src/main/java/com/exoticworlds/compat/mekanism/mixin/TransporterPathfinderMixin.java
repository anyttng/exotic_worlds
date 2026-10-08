package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.MekanismInjectionTargets;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import it.unimi.dsi.fastutil.longs.LongList;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterPathfinder;
import mekanism.common.lib.SidedBlockPos;
import mekanism.common.lib.inventory.IAdvancedTransportEjector;
import mekanism.common.util.WorldUtils;

import net.minecraft.core.Direction;

@Mixin(value = TransporterPathfinder.class, remap = false)
public class TransporterPathfinderMixin {
    private static final int DESTINATION_INDEX = 0;
    private static final int LAST_TRANSPORTER_INDEX = 1;

    @WrapOperation(method = "getNewRRPath",
            at = @At(value = "INVOKE", target = MekanismInjectionTargets.WORLD_UTILS_SIDE_DIFFERENCE_PACKED))
    private static Direction toroidal$sideOfDestination(long pos, long other, Operation<Direction> original,
            @Local(argsOnly = true) LogisticalTransporterBase start) {
        return original.call(MekanismSeam.nearestCopy(start.getLevel(), other, pos), other);
    }

    @WrapOperation(method = "getNewRRPath",
            at = @At(value = "INVOKE", target = "Lmekanism/common/lib/inventory/IAdvancedTransportEjector;"
                    + "setRoundRobinTarget(Lmekanism/common/content/transporter/TransporterPathfinder$Destination;)V"),
            require = 4,
            expect = 4)
    private static void toroidal$roundRobinSideOfDestination(IAdvancedTransportEjector outputter,
            TransporterPathfinder.Destination destination, Operation<Void> original,
            @Local(argsOnly = true) LogisticalTransporterBase start) {
        LongList path = destination.getPath();
        long dest = path.getLong(DESTINATION_INDEX);
        long lastTransporter = MekanismSeam.nearestCopy(start.getLevel(), dest, path.getLong(LAST_TRANSPORTER_INDEX));
        outputter.setRoundRobinTarget(new SidedBlockPos(dest, WorldUtils.sideDifference(lastTransporter, dest)));
    }
}
