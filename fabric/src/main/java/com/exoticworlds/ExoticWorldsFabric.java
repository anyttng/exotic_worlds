package com.exoticworlds;

import com.exoticworlds.engine.gen.LoopedChunkGenerator;
import com.exoticworlds.engine.gen.LoopedFlatChunkGenerator;
import com.exoticworlds.engine.gen.WorldLoopGenerators;
import com.exoticworlds.engine.gen.WorldLoopTicketTypes;
import com.exoticworlds.engine.net.OpenMenuTranslation;
import com.exoticworlds.engine.net.PositionRowsPayload;
import com.exoticworlds.engine.net.PositionRowsReloadListener;
import com.exoticworlds.engine.net.PositionRowsSync;
import com.exoticworlds.engine.net.WrappingSettingsPayload;
import com.exoticworlds.engine.seam.circumnavigation.WorldLoopCriteria;
import com.exoticworlds.platform.FabricPlatform;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.GenerationHookSetup;
import com.exoticworlds.shape.WorldOptionSetup;
import com.exoticworlds.shape.WorldShapeSetup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class ExoticWorldsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ExoticWorlds.LOGGER.info("Exotic Worlds initializing");
        Platforms.set(new FabricPlatform());
        WorldOptionSetup.registerAll();
        WorldShapeSetup.registerAll();
        GenerationHookSetup.registerAll();

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, WorldLoopGenerators.TOROIDAL_ID),
                LoopedChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, WorldLoopGenerators.TOROIDAL_FLAT_ID),
                LoopedFlatChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.TICKET_TYPE,
                Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, WorldLoopTicketTypes.SEAM_GENERATION_ID),
                WorldLoopTicketTypes.SEAM_GENERATION);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, WorldLoopCriteria.CIRCUMNAVIGATE_ID),
                WorldLoopCriteria.CIRCUMNAVIGATE);

        PayloadTypeRegistry.clientboundPlay().register(WrappingSettingsPayload.TYPE, WrappingSettingsPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PositionRowsPayload.TYPE, PositionRowsPayload.STREAM_CODEC);

        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(PositionRowsReloadListener.ID,
                new PositionRowsReloadListener());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> PositionRowsSync.sendTo(player));

        OpenMenuTranslation.register();
    }
}
