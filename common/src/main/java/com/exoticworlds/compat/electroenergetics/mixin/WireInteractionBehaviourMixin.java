package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.wire.interaction.WireInteractionBehaviour;
import com.george_vi.electroenergetics.foundation.nodes.NodeConnectionPoint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.electroenergetics.ElectroEnergeticsInjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WireInteractionBehaviour.class, remap = false)
public abstract class WireInteractionBehaviourMixin {
    @WrapOperation(method = "attachToWire", at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double toroidal$attachmentSpan(BlockPos from, Vec3i to, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return original.call(from, WireSpan.seat(level, from, to));
    }

    @WrapOperation(method = "attachToWire",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.CONNECTION_POINT_POS_AT))
    private Vec3 toroidal$attachmentSoundOnShortSpan(NodeConnectionPoint point, Vec3 from, Vec3 to, float dip,
            Operation<Vec3> original, @Local(argsOnly = true) Level level) {
        return original.call(point, from, WireSpan.seat(level, from, to), dip);
    }
}
