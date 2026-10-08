package com.exoticworlds;

import com.exoticworlds.compat.aeronautics.AeronauticsTranslation;
import com.exoticworlds.compat.create.CreateTranslation;
import com.exoticworlds.compat.aeronautics.AeronauticsMod;
import com.exoticworlds.compat.sable.SableMod;
import com.exoticworlds.engine.gen.LoopedChunkGenerator;
import com.exoticworlds.engine.gen.LoopedFlatChunkGenerator;
import com.exoticworlds.engine.gen.WorldLoopGenerators;
import com.exoticworlds.engine.net.FabricPositionRowsReloadListener;
import com.exoticworlds.engine.net.OpenMenuTranslation;
import com.exoticworlds.engine.net.PositionRowsPayload;
import com.exoticworlds.engine.net.PositionRowsSync;
import com.exoticworlds.engine.net.WrappingSettingsPayload;
import com.exoticworlds.engine.seam.circumnavigation.WorldLoopCriteria;
import com.exoticworlds.shape.WorldOptionSetup;
import com.exoticworlds.platform.FabricPlatform;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.GenerationHookSetup;
import com.exoticworlds.shape.WorldShapeSetup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ExoticWorldsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ExoticWorlds.LOGGER.info("Exotic Worlds initializing");
        Platforms.set(new FabricPlatform());
        WorldOptionSetup.registerAll();
        WorldShapeSetup.registerAll();
        SableMod.register();
        AeronauticsMod.register();
        CreateTranslation.register();
        AeronauticsTranslation.register();
        GenerationHookSetup.registerAll();

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, WorldLoopGenerators.TOROIDAL_ID),
                LoopedChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, WorldLoopGenerators.TOROIDAL_FLAT_ID),
                LoopedFlatChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, WorldLoopCriteria.CIRCUMNAVIGATE_ID),
                WorldLoopCriteria.CIRCUMNAVIGATE);

        PayloadTypeRegistry.playS2C().register(WrappingSettingsPayload.TYPE, WrappingSettingsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PositionRowsPayload.TYPE, PositionRowsPayload.STREAM_CODEC);

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricPositionRowsReloadListener());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> PositionRowsSync.sendTo(player));

        OpenMenuTranslation.register();
    }
}
