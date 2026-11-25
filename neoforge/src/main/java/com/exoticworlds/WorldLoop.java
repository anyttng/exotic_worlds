package com.exoticworlds;

import com.exoticworlds.compat.moonlight.MoonlightParticles;
import com.exoticworlds.compat.northbound.NorthboundLatitude;
import com.exoticworlds.engine.gen.LegacyGenerators;
import com.exoticworlds.engine.net.AuxiliaryLightTranslation;
import com.exoticworlds.engine.net.BlockParticleTranslation;
import com.exoticworlds.engine.net.SpawnBufferTranslation;
import com.exoticworlds.engine.seam.circumnavigation.WorldLoopCriteria;
import com.exoticworlds.platform.NeoForgePlatform;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.settings.SettingsService;
import com.exoticworlds.settings.client.SettingsScreenFactory;
import com.exoticworlds.shape.StartupSetup;
import com.mojang.serialization.MapCodec;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WorldLoop {
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ExoticWorlds.MODID);

    private static final DeferredRegister<CriterionTrigger<?>> CRITERIA =
            DeferredRegister.create(Registries.TRIGGER_TYPE, ExoticWorlds.MODID);

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        Platforms.set(new NeoForgePlatform(modContainer));
        StartupSetup.registerAll();
        NorthboundLatitude.register();
        MoonlightParticles.register();

        CHUNK_GENERATORS.register(LegacyGenerators.TOROIDAL_ID, () -> LegacyGenerators.TOROIDAL_CODEC);
        CHUNK_GENERATORS.register(LegacyGenerators.TOROIDAL_FLAT_ID, () -> LegacyGenerators.TOROIDAL_FLAT_CODEC);
        CHUNK_GENERATORS.register(modEventBus);

        CRITERIA.register(WorldLoopCriteria.CIRCUMNAVIGATE_ID, () -> WorldLoopCriteria.CIRCUMNAVIGATE);
        CRITERIA.register(modEventBus);

        AuxiliaryLightTranslation.register();
        BlockParticleTranslation.register();
        SpawnBufferTranslation.register();
        if (Platforms.get().isClient()) {
            SettingsService.set(SettingsService.load(Platforms.get().configDir()));
            SettingsScreenFactory.register(modContainer);
        }
    }

    private WorldLoop() {
    }
}
