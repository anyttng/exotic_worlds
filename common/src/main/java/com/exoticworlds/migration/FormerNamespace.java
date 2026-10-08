package com.exoticworlds.migration;

import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class FormerNamespace {
    public static final String NAMESPACE = "toroidal_world";
    public static final String KEY_PREFIX = NAMESPACE + ":";

    private static final String CURRENT_KEY_PREFIX = ExoticWorlds.MODID + ":";

    private static final String TORUS_PRESET_PREFIX = "torus_";
    private static final Set<String> BARE_TORUS_PRESETS = Set.of("tiny", "small", "medium", "large", "huge");

    public static @Nullable ResourceLocation twinOf(@Nullable ResourceLocation id) {
        if (id == null || !NAMESPACE.equals(id.getNamespace())) {
            return null;
        }

        String path = id.getPath();
        return ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID,
                BARE_TORUS_PRESETS.contains(path) ? TORUS_PRESET_PREFIX + path : path);
    }

    public static <T> @Nullable ResourceKey<T> twinOf(@Nullable ResourceKey<T> key) {
        ResourceLocation twin = key == null ? null : twinOf(key.location());
        return twin == null ? null : ResourceKey.create(key.registryKey(), twin);
    }

    public static @Nullable String twinKeyOf(String key) {
        ResourceLocation twin = twinOf(ResourceLocation.tryParse(key));
        return twin == null ? null : twin.toString();
    }

    public static String formerKey(String key) {
        if (!key.startsWith(CURRENT_KEY_PREFIX)) {
            throw new IllegalArgumentException("Not a key of ours: " + key);
        }

        return KEY_PREFIX + key.substring(CURRENT_KEY_PREFIX.length());
    }

    public static Optional<CompoundTag> compound(CompoundTag tag, String key) {
        String stored = tag.contains(key, Tag.TAG_COMPOUND) ? key : formerKey(key);
        return tag.contains(stored, Tag.TAG_COMPOUND) ? Optional.of(tag.getCompound(stored)) : Optional.empty();
    }

    private FormerNamespace() {
    }
}
