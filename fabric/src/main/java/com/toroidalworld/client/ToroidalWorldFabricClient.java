package com.toroidalworld.client;

import com.toroidalworld.client.engine.WorldLoopClientNetwork;
import com.toroidalworld.engine.net.PositionRowsPayload;
import com.toroidalworld.engine.net.WrappingSettingsPayload;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.settings.SettingsService;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ToroidalWorldFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SettingsService.set(SettingsService.load(Platforms.get().configDir()));

        ClientPlayNetworking.registerGlobalReceiver(WrappingSettingsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> WorldLoopClientNetwork.apply(payload.dimension(), payload.shape())));
        ClientPlayNetworking.registerGlobalReceiver(PositionRowsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> WorldLoopClientNetwork.declare(payload)));
    }
}
