package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.linemans_stick.LinemansStickWireInteractionBehaviour;
import com.george_vi.electroenergetics.content.wire.attachments.AttachmentRemovalWireInteractionBehaviour;
import com.george_vi.electroenergetics.foundation.nodes.NodeConnectionPoint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.electroenergetics.ElectroEnergeticsInjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = {AttachmentRemovalWireInteractionBehaviour.class, LinemansStickWireInteractionBehaviour.class},
        remap = false)
public abstract class AttachmentInteractionMixin {
    @WrapOperation(method = "interactWire", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double toroidal$attachmentSpan(Vec3 from, Vec3 to, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return original.call(from, WireSpan.seat(level, from, to));
    }

    @WrapOperation(method = "interactWire",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.CONNECTION_POINT_POS_AT))
    private Vec3 toroidal$soundOnShortSpan(NodeConnectionPoint point, Vec3 from, Vec3 to, float dip,
            Operation<Vec3> original, @Local(argsOnly = true) Level level) {
        return original.call(point, from, WireSpan.seat(level, from, to), dip);
    }

    @WrapOperation(method = "interactWire",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.WIRE_POS_AT))
    private Vec3 toroidal$dropOnShortSpan(Vec3 from, Vec3 to, float point, float dip, Operation<Vec3> original,
            @Local(argsOnly = true) Level level) {
        return original.call(from, WireSpan.seat(level, from, to), point, dip);
    }
}
