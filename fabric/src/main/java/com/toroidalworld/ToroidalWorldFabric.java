package com.toroidalworld;

import com.toroidalworld.compat.aeronautics.AeronauticsTranslation;
import com.toroidalworld.compat.create.CreateTranslation;
import com.toroidalworld.compat.aeronautics.AeronauticsMod;
import com.toroidalworld.compat.sable.SableMod;
import com.toroidalworld.engine.gen.LoopedChunkGenerator;
import com.toroidalworld.engine.gen.LoopedFlatChunkGenerator;
import com.toroidalworld.engine.gen.WorldLoopGenerators;
import com.toroidalworld.engine.net.FabricPositionRowsReloadListener;
import com.toroidalworld.engine.net.OpenMenuTranslation;
import com.toroidalworld.engine.net.PositionRowsPayload;
import com.toroidalworld.engine.net.PositionRowsSync;
import com.toroidalworld.engine.net.WrappingSettingsPayload;
import com.toroidalworld.engine.seam.circumnavigation.WorldLoopCriteria;
import com.toroidalworld.shape.WorldOptionSetup;
import com.toroidalworld.platform.FabricPlatform;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.GenerationHookSetup;
import com.toroidalworld.shape.WorldShapeSetup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ToroidalWorldFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ToroidalWorld.LOGGER.info("Toroidal World initializing");
        Platforms.set(new FabricPlatform());
        WorldOptionSetup.registerAll();
        WorldShapeSetup.registerAll();
        SableMod.register();
        AeronauticsMod.register();
        CreateTranslation.register();
        AeronauticsTranslation.register();
        GenerationHookSetup.registerAll();

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_ID),
                LoopedChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_FLAT_ID),
                LoopedFlatChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopCriteria.CIRCUMNAVIGATE_ID),
                WorldLoopCriteria.CIRCUMNAVIGATE);

        PayloadTypeRegistry.playS2C().register(WrappingSettingsPayload.TYPE, WrappingSettingsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PositionRowsPayload.TYPE, PositionRowsPayload.STREAM_CODEC);

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricPositionRowsReloadListener());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> PositionRowsSync.sendTo(player));

        OpenMenuTranslation.register();
    }
}
