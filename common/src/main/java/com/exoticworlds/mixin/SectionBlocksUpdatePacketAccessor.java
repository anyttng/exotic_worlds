package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public interface SectionBlocksUpdatePacketAccessor {
    @Accessor("sectionPos")
    SectionPos exotic_worlds$getSectionPos();

    @Accessor("positions")
    short[] exotic_worlds$getPositions();

    @Accessor("states")
    BlockState[] exotic_worlds$getStates();

    @Invoker("write")
    void exotic_worlds$write(FriendlyByteBuf output);

    @Invoker("<init>")
    static ClientboundSectionBlocksUpdatePacket exotic_worlds$create(FriendlyByteBuf input) {
        throw new AssertionError();
    }
}
