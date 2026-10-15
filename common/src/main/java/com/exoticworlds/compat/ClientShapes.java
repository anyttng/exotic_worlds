package com.exoticworlds.compat;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.ExoticWorldsClientApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ClientShapes {
    public static @Nullable ToroidalShape current() {
        return of(Minecraft.getInstance().level);
    }

    public static @Nullable ToroidalShape of(Level level) {
        return level instanceof ClientLevel client ? ExoticWorldsClientApi.shapeOf(client).orElse(null) : null;
    }

    public static BlockPos fold(@Nullable ToroidalShape shape, BlockPos pos) {
        return shape == null ? pos : shape.fold(pos);
    }

    public static Vec3 fold(@Nullable ToroidalShape shape, Vec3 point) {
        return shape == null ? point : shape.fold(point);
    }

    public static ChunkPos fold(@Nullable ToroidalShape shape, ChunkPos chunk) {
        return shape == null ? chunk : shape.fold(chunk);
    }

    private ClientShapes() {
    }
}
