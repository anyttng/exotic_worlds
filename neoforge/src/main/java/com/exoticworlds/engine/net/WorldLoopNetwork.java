package com.exoticworlds.engine.net;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.client.engine.WorldLoopClientNetwork;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ExoticWorlds.MODID)
public final class WorldLoopNetwork {
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    @SuppressWarnings("FutureReturnValueIgnored")
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
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PositionRowsReloadListener());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(PositionRowsSync::sendTo);
    }

    private WorldLoopNetwork() {
    }
}
