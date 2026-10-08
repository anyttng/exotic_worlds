package com.exoticworlds.migration;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class FormerDataDirectory extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public FormerDataDirectory() {
        super(GSON, FormerNamespace.NAMESPACE);
    }

    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        return super.prepare(manager, profiler);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
    }
}
