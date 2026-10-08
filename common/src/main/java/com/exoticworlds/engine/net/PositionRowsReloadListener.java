package com.exoticworlds.engine.net;

import java.util.HashMap;
import java.util.Map;

import com.exoticworlds.ExoticWorlds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class PositionRowsReloadListener extends SimpleJsonResourceReloadListener<PositionRows> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, PositionRows.FILE_NAME);

    public PositionRowsReloadListener() {
        super(PositionRows.CODEC, FileToIdConverter.json(PositionRows.DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, PositionRows> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, PositionRows> positionFiles = new HashMap<>();
        files.forEach((file, rows) -> {
            if (file.getPath().equals(PositionRows.FILE_NAME)) {
                positionFiles.put(file, rows);
            }
        });

        PositionRowsSync.apply(PositionRows.merge(positionFiles,
                BuiltInRegistries.BLOCK_ENTITY_TYPE::containsKey, BuiltInRegistries.ENTITY_TYPE::containsKey,
                BuiltInRegistries.DATA_COMPONENT_TYPE::containsKey));
    }
}
