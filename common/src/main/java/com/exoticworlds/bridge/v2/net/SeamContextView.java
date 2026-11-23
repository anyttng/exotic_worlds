package com.exoticworlds.bridge.v2.net;

import com.exoticworlds.api.v2.geometry.WorldGeometryV2;
import com.exoticworlds.api.v2.net.SeamContextV2;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.engine.net.TranslationContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

record SeamContextView(TranslationContext context) implements SeamContextV2 {

    @Override
    public WorldGeometryV2 geometry() {
        return GeometryBridge.of(this.context.fold());
    }

    @Override
    public BlockPos toClient(BlockPos pos) {
        return this.context.toClient(pos);
    }

    @Override
    public Vec3 toClient(Vec3 pos) {
        return this.context.toClient(pos);
    }

    @Override
    public ChunkPos toClient(ChunkPos chunkPos) {
        return this.context.toClient(chunkPos);
    }

    @Override
    public BlockPos toServer(BlockPos pos) {
        return this.context.toServer(pos);
    }

    @Override
    public Vec3 toServer(Vec3 pos) {
        return this.context.toServer(pos);
    }

    @Override
    public ChunkPos toServer(ChunkPos chunkPos) {
        return this.context.toServer(chunkPos);
    }
}
