package com.exoticworlds.engine.noise;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exoticworlds.ExoticWorlds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class SeamNoticeRecord extends SavedData {
    private static final String ID = ExoticWorlds.MODID + "_edge_notices";
    private static final String SHOWN_KEY = "shown";

    private static final Codec<SeamNoticeRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf(SHOWN_KEY).forGetter(SeamNoticeRecord::shown))
            .apply(instance, SeamNoticeRecord::new));

    // Vanilla's reader calls the fix type unguarded, so a null here loses the record on every Fabric load.
    private static final SavedData.Factory<SeamNoticeRecord> TYPE = new SavedData.Factory<>(
            SeamNoticeRecord::new, SeamNoticeRecord::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Set<String> shown;

    private SeamNoticeRecord() {
        this(List.of());
    }

    private SeamNoticeRecord(List<String> shown) {
        this.shown = new LinkedHashSet<>(shown);
    }

    public static SeamNoticeRecord of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE, ID);
    }

    private static SeamNoticeRecord load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(SeamNoticeRecord::new);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, this).result()
                .ifPresent(encoded -> tag.merge((CompoundTag) encoded));
        return tag;
    }

    boolean contains(String fingerprint) {
        return this.shown.contains(fingerprint);
    }

    void add(Collection<String> fingerprints) {
        if (this.shown.addAll(fingerprints)) {
            this.setDirty();
        }
    }

    private List<String> shown() {
        return List.copyOf(this.shown);
    }
}
