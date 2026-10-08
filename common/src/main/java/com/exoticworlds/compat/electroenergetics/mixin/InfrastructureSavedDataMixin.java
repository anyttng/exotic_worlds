package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.electroenergetics.ElectroEnergeticsInjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

@Mixin(value = InfrastructureSavedData.class, remap = false)
public abstract class InfrastructureSavedDataMixin {
    @Shadow
    @Final
    public ServerLevel level;

    @WrapOperation(method = "removeConnection",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.WIRE_POS_AT))
    private Vec3 toroidal$attachmentDropOnShortSpan(Vec3 from, Vec3 to, float point, float dip,
            Operation<Vec3> original) {
        return original.call(from, WireSpan.seat(this.level, from, to), point, dip);
    }

    @WrapOperation(method = "removeAndDropConnection",
            at = @At(value = "INVOKE", target = "Lcom/george_vi/electroenergetics/foundation/QuadraticWireHelper;posAt"
                    + "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$wireDropOnShortSpan(Vec3 from, Vec3 to, float point, Operation<Vec3> original) {
        return original.call(from, WireSpan.seat(this.level, from, to), point);
    }
}
