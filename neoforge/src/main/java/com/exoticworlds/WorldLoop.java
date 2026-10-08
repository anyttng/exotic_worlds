package com.exoticworlds;

import com.exoticworlds.client.settings.SettingsScreenFactory;
import com.exoticworlds.compat.aeronautics.AeronauticsTranslation;
import com.exoticworlds.compat.create.CreateTranslation;
import com.exoticworlds.compat.electroenergetics.ElectroEnergeticsTranslation;
import com.exoticworlds.compat.aeronautics.AeronauticsMod;
import com.exoticworlds.compat.sable.SableMod;
import com.exoticworlds.engine.gen.LoopedChunkGenerator;
import com.exoticworlds.engine.gen.LoopedFlatChunkGenerator;
import com.exoticworlds.engine.gen.WorldLoopGenerators;
import com.exoticworlds.engine.net.AuxiliaryLightTranslation;
import com.exoticworlds.engine.net.BlockParticleTranslation;
import com.exoticworlds.engine.net.SpawnBufferTranslation;
import com.exoticworlds.engine.seam.circumnavigation.WorldLoopCriteria;
import com.exoticworlds.shape.WorldOptionSetup;
import com.exoticworlds.platform.NeoForgePlatform;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.settings.SettingsService;
import com.exoticworlds.shape.GenerationHookSetup;
import com.exoticworlds.shape.WorldShapeSetup;
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
        WorldOptionSetup.registerAll();
        WorldShapeSetup.registerAll();
        SableMod.register();
        AeronauticsMod.register();
        CreateTranslation.register();
        AeronauticsTranslation.register();
        ElectroEnergeticsTranslation.register();
        GenerationHookSetup.registerAll();

        CHUNK_GENERATORS.register(WorldLoopGenerators.TOROIDAL_ID, () -> LoopedChunkGenerator.CODEC);
        CHUNK_GENERATORS.register(WorldLoopGenerators.TOROIDAL_FLAT_ID, () -> LoopedFlatChunkGenerator.CODEC);
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
