package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.lib.transmitter.DynamicNetwork;

import net.minecraft.world.level.Level;

@Mixin(value = DynamicNetwork.class, remap = false)
public abstract class DynamicNetworkMixin {
    @Shadow
    protected Level world;

    @ModifyVariable(method = "getTransmitter(J)Lmekanism/common/content/network/transmitter/Transmitter;",
            at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldTransmitterKey(long pos) {
        return MekanismSeam.fold(this.world, pos);
    }
}
