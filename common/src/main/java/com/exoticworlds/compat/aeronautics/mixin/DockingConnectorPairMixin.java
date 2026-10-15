package com.exoticworlds.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.aeronautics.MagnetSeamFrame;

import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorPair;

import net.minecraft.world.phys.Vec3;

@Mixin(value = DockingConnectorPair.class, remap = false)
public class DockingConnectorPairMixin {
    private static final int PROJECTED_TIP_1 = 2;
    private static final int PROJECTED_TIP_2 = 3;

    @ModifyReturnValue(method = "getAverageTipPosition", at = @At("RETURN"))
    private static Vector3d toroidal$foldTipMidpoint(Vector3d average, DockingConnectorBlockEntity dock1,
            @Local(ordinal = PROJECTED_TIP_1) Vec3 tip1, @Local(ordinal = PROJECTED_TIP_2) Vec3 tip2) {
        return MagnetSeamFrame.midpoint(dock1.getLevel(), tip1, tip2, average);
    }
}
