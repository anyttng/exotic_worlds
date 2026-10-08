package com.exoticworlds.client;

import com.exoticworlds.client.engine.WorldLoopClientNetwork;
import com.exoticworlds.engine.net.PositionRowsPayload;
import com.exoticworlds.engine.net.WrappingSettingsPayload;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.settings.SettingsService;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ExoticWorldsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SettingsService.set(SettingsService.load(Platforms.get().configDir()));

        ClientPlayNetworking.registerGlobalReceiver(WrappingSettingsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> WorldLoopClientNetwork.apply(payload.dimension(), payload.shape())));
        ClientPlayNetworking.registerGlobalReceiver(PositionRowsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> WorldLoopClientNetwork.declare(payload)));
    }
}
