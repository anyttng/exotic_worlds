package com.toroidalworld.engine.net;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.client.engine.WorldLoopClientNetwork;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ToroidalWorld.MODID)
public final class WorldLoopNetwork {
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        registrar.playToClient(
                WrappingSettingsPayload.TYPE,
                WrappingSettingsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> WorldLoopClientNetwork.apply(payload.dimension(), payload.shape())));
        registrar.playToClient(
                PositionRowsPayload.TYPE,
                PositionRowsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> WorldLoopClientNetwork.declare(payload)));
    }

    @SubscribeEvent
    static void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(PositionRowsReloadListener.ID, new PositionRowsReloadListener());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(PositionRowsSync::sendTo);
    }

    private WorldLoopNetwork() {
    }
}
