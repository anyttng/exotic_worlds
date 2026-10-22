package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.hollingsworth.arsnouveau.common.network.ChangeBiomePacket;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

@Mixin(value = ChangeBiomePacket.class, remap = false)
public interface ChangeBiomePacketAccessor {
    @Accessor("pos")
    BlockPos exotic_worlds$pos();

    @Accessor("biomeId")
    ResourceKey<Biome> exotic_worlds$biomeId();
}
