package com.exoticworlds.engine.noise;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.exoticworlds.accessors.SeamDetailsPendingHolder;

import net.minecraft.server.MinecraftServer;

public final class SeamDetailsPending {
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();

    static SeamDetailsPending of(MinecraftServer server) {
        return ((SeamDetailsPendingHolder) server).exotic_worlds$seamDetailsPending();
    }

    void mark(UUID player) {
        this.players.add(player);
    }

    boolean take(UUID player) {
        return this.players.remove(player);
    }
}
