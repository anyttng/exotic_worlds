package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.george_vi.electroenergetics.simulation.infrastructure.CatenaryModule;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

@Mixin(value = CatenaryModule.class, remap = false)
public abstract class CatenaryModuleMixin {
    @Shadow
    @Final
    ServerLevel level;

    @ModifyVariable(method = "buildCircuit", at = @At("STORE"), name = "coupledVec")
    private Vec3 toroidal$trailingAnchorBesideLeading(Vec3 coupledVec, @Local(name = "positionVec") Vec3 positionVec) {
        return WireSpan.seatIfPresent(this.level, positionVec, coupledVec);
    }

    @ModifyVariable(method = "buildCircuit", at = @At("STORE"), name = "start")
    private Vec3 toroidal$wireStartBesideCollector(Vec3 start, @Local(name = "pantographPos") Vec3 collector) {
        return WireSpan.seat(this.level, collector, start);
    }

    @ModifyVariable(method = "buildCircuit", at = @At("STORE"), name = "end")
    private Vec3 toroidal$wireEndBesideStart(Vec3 end, @Local(name = "start") Vec3 start) {
        return WireSpan.seat(this.level, start, end);
    }
}
