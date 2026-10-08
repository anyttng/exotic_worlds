package com.exoticworlds.migration;

import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class FormerDataDirectory<T> extends SimpleJsonResourceReloadListener<T> {
    public FormerDataDirectory(Codec<T> codec) {
        super(codec, FileToIdConverter.json(FormerNamespace.NAMESPACE));
    }

    @Override
    public Map<Identifier, T> prepare(ResourceManager manager, ProfilerFiller profiler) {
        return super.prepare(manager, profiler);
    }

    @Override
    protected void apply(Map<Identifier, T> files, ResourceManager manager, ProfilerFiller profiler) {
    }
}
