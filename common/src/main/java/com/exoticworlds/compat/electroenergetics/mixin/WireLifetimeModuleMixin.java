package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.simulation.infrastructure.WireLifetimeModule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CatnipInjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WireLifetimeModule.class, remap = false)
public abstract class WireLifetimeModuleMixin {
    @Shadow
    @Final
    ServerLevel level;

    @WrapOperation(method = "finishSimulation",
            at = @At(value = "INVOKE", target = CatnipInjectionTargets.VEC_HELPER_LERP))
    private Vec3 toroidal$centreOnShortSpan(float point, Vec3 from, Vec3 to, Operation<Vec3> original) {
        return original.call(point, from, WireSpan.seat(this.level, from, to));
    }

    @WrapOperation(method = "finishSimulation",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double toroidal$shortSpan(Vec3 from, Vec3 to, Operation<Double> original) {
        return original.call(from, WireSpan.seat(this.level, from, to));
    }
}
