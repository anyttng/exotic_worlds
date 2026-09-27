package com.toroidalworld.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.rotor.AlternatorBrushesDevice;
import com.george_vi.electroenergetics.content.rotor.ThreePhaseAlternatorBrushesDevice;
import com.george_vi.electroenergetics.content.transmission_distribution.hv_switch.HVSwitchDevice;
import com.george_vi.electroenergetics.devices.device.SimulatedDevice;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.toroidalworld.compat.create.CatnipInjectionTargets;
import com.toroidalworld.compat.electroenergetics.WireSpan;

import net.minecraft.core.BlockPos;

@Mixin(value = {HVSwitchDevice.class, AlternatorBrushesDevice.class, ThreePhaseAlternatorBrushesDevice.class},
        remap = false)
public abstract class DevicePartnerReadMixin {
    @ModifyExpressionValue(method = "read",
            at = @At(value = "INVOKE", target = CatnipInjectionTargets.NBT_HELPER_READ_BLOCK_POS))
    private BlockPos toroidal$canonicalPartner(BlockPos partner) {
        return WireSpan.block(((SimulatedDevice) (Object) this).level, partner);
    }
}
