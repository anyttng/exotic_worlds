package com.exoticworlds.engine.gen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

public final class TerrainMasks {
    private static final int WINDOW_CONSUMERS = 9;

    private static final String MASK_KEY = "mask";

    private static final String REMAINING_KEY = "remaining";

    private record Held(TerrainMask mask, AtomicInteger remaining) {
    }

    private final Map<Long, Held> held = new ConcurrentHashMap<>();

    public void put(ChunkPos pos, TerrainMask mask) {
        this.held.put(pos.toLong(), new Held(mask, new AtomicInteger(WINDOW_CONSUMERS)));
    }

    @Nullable TerrainMask at(long key) {
        Held found = this.held.get(key);
        return found != null ? found.mask() : null;
    }

    boolean consumed(long key) {
        Held found = this.held.get(key);
        if (found == null) {
            return false;
        }

        if (found.remaining().decrementAndGet() <= 0) {
            this.held.remove(key);
        }

        return true;
    }

    @Nullable CompoundTag saved(ChunkPos pos) {
        Held found = this.held.get(pos.toLong());
        if (found == null) {
            return null;
        }

        CompoundTag tag = new CompoundTag();
        tag.put(MASK_KEY, found.mask().save());
        tag.putInt(REMAINING_KEY, found.remaining().get());
        return tag;
    }

    @Nullable TerrainMask restore(ChunkPos pos, CompoundTag tag, int minY, int height) {
        int remaining = tag.getInt(REMAINING_KEY);
        TerrainMask mask = tag.contains(MASK_KEY, Tag.TAG_COMPOUND)
                ? TerrainMask.load(tag.getCompound(MASK_KEY), pos, minY, height)
                : null;
        if (mask == null || remaining <= 0) {
            return null;
        }

        this.held.put(pos.toLong(), new Held(mask, new AtomicInteger(remaining)));
        return mask;
    }

    void release(ChunkPos pos) {
        this.held.remove(pos.toLong());
    }
}
