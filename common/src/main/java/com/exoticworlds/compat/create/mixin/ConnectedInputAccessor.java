package com.exoticworlds.compat.create.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.zurrtum.create.content.kinetics.crafter.ConnectedInputHandler;

import net.minecraft.core.BlockPos;

@Mixin(ConnectedInputHandler.ConnectedInput.class)
public interface ConnectedInputAccessor {
    @Accessor("data")
    List<BlockPos> exotic_worlds$data();

    @Accessor("isController")
    boolean exotic_worlds$isController();
}
