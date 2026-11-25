package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.network.packet.ClientBlockTargetCastPacket;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;

import net.minecraft.resources.ResourceLocation;

@Mixin(value = ClientBlockTargetCastPacket.class, remap = false)
public interface ClientBlockTargetCastPacketAccessor {
    @Accessor("quickCastSlot")
    int exotic_worlds$getQuickCastSlot();

    @Accessor("spellId")
    ResourceLocation exotic_worlds$getSpellId();

    @Accessor("targetData")
    BlockTargetData exotic_worlds$getTargetData();

    @Accessor("initiateCast")
    boolean exotic_worlds$shouldInitiateCast();
}
