package com.toroidalworld.engine.net;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.toroidalworld.ToroidalWorld;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class PositionRowsReloadListener extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, PositionRows.FILE_NAME);

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new Gson();

    public PositionRowsReloadListener() {
        super(GSON, PositionRows.DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, PositionRows> positionFiles = new HashMap<>();
        files.forEach((file, json) -> PositionRows.CODEC.parse(JsonOps.INSTANCE, json)
                .ifSuccess(rows -> {
                    if (file.getPath().equals(PositionRows.FILE_NAME)) {
                        positionFiles.put(file, rows);
                    }
                })
                .ifError(error -> LOGGER.error("Couldn't parse data file '{}': {}", file, error.message())));

        PositionRowsSync.apply(PositionRows.merge(positionFiles,
                BuiltInRegistries.BLOCK_ENTITY_TYPE::containsKey, BuiltInRegistries.ENTITY_TYPE::containsKey,
                BuiltInRegistries.DATA_COMPONENT_TYPE::containsKey));
    }
}
