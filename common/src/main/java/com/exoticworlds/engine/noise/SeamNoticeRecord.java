package com.exoticworlds.engine.noise;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.exoticworlds.ExoticWorlds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class SeamNoticeRecord extends SavedData {
    private static final String ID = "edge_notices";
    private static final String SHOWN_KEY = "shown";

    private static final Codec<SeamNoticeRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf(SHOWN_KEY).forGetter(SeamNoticeRecord::shown))
            .apply(instance, SeamNoticeRecord::new));

    // Vanilla's reader calls the fix type unguarded, so a null here loses the record on every Fabric load.
    private static final SavedDataType<SeamNoticeRecord> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, ID), SeamNoticeRecord::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Set<String> shown;
    private final Set<UUID> detailsPending = ConcurrentHashMap.newKeySet();

    private SeamNoticeRecord() {
        this(List.of());
    }

    private SeamNoticeRecord(List<String> shown) {
        this.shown = new LinkedHashSet<>(shown);
    }

    public static SeamNoticeRecord of(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    boolean contains(String fingerprint) {
        return this.shown.contains(fingerprint);
    }

    void add(Collection<String> fingerprints) {
        if (this.shown.addAll(fingerprints)) {
            this.setDirty();
        }
    }

    void markDetailsPending(UUID player) {
        this.detailsPending.add(player);
    }

    boolean takeDetailsPending(UUID player) {
        return this.detailsPending.remove(player);
    }

    private List<String> shown() {
        return List.copyOf(this.shown);
    }
}
