package com.exoticworlds;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(ExoticWorlds.MODID)
public class ExoticWorldsNeoForge {
    public ExoticWorldsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ExoticWorlds.LOGGER.info("Exotic Worlds initializing");

        WorldLoop.init(modEventBus, modContainer);
    }
}
