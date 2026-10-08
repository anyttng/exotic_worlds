package com.exoticworlds.engine.net;

import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class SpawnBufferFold {
    private static final TagPositions.Table TABLE = new TagPositions.Table("Spawn buffer positions");

    public static void register(Class<?> entityType, TagPositions.PositionShape shape, String... keys) {
        TABLE.register(entityType, shape, keys);
    }

    static void declare(Map<ResourceLocation, List<TagPositions.TagPosition>> rows) {
        TABLE.declare(rows);
    }

    public static boolean carriesPositions(TagPositions.Subject entity) {
        return TABLE.carriesPositions(entity);
    }

    public static CompoundTag seatedIn(TagPositions.Seat seat, TagPositions.Subject entity, CompoundTag tag) {
        return TABLE.seatedIn(seat, entity, tag);
    }

    private SpawnBufferFold() {
    }
}
