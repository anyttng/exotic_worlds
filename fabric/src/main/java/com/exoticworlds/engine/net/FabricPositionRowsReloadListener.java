package com.exoticworlds.engine.net;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public final class FabricPositionRowsReloadListener implements IdentifiableResourceReloadListener {
    private final PositionRowsReloadListener rows = new PositionRowsReloadListener();

    @Override
    public ResourceLocation getFabricId() {
        return PositionRowsReloadListener.ID;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
            ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor,
            Executor gameExecutor) {
        return rows.reload(barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
    }
}
