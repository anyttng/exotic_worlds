package com.exoticworlds.compat.vista;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.ChunkPos;

public interface ZoneSeats {
    @Nullable List<ChunkPos> exotic_worlds$sentSeats();

    void exotic_worlds$setSentSeats(List<ChunkPos> seats);
}
