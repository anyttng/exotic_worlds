package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.wire_spool.ChangeLengthWireInteractionBehaviour;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CatnipInjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ChangeLengthWireInteractionBehaviour.class, remap = false)
public abstract class ChangeLengthWireInteractionBehaviourMixin {
    @WrapOperation(method = "interactWire", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double toroidal$shortSpan(Vec3 from, Vec3 to, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return original.call(from, WireSpan.seat(level, from, to));
    }

    @WrapOperation(method = "interactWire",
            at = @At(value = "INVOKE", target = CatnipInjectionTargets.VEC_HELPER_LERP))
    private Vec3 toroidal$dropOnShortSpan(float point, Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local(argsOnly = true) Level level) {
        return original.call(point, from, WireSpan.seat(level, from, to));
    }
}
