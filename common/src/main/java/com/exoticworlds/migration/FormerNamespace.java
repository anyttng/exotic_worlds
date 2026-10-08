package com.exoticworlds.migration;

import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class FormerNamespace {
    public static final String NAMESPACE = "toroidal_world";
    public static final String KEY_PREFIX = NAMESPACE + ":";

    private static final String CURRENT_KEY_PREFIX = ExoticWorlds.MODID + ":";

    private static final String TORUS_PRESET_PREFIX = "torus_";
    private static final Set<String> BARE_TORUS_PRESETS = Set.of("tiny", "small", "medium", "large", "huge");

    public static @Nullable Identifier twinOf(@Nullable Identifier id) {
        if (id == null || !NAMESPACE.equals(id.getNamespace())) {
            return null;
        }

        String path = id.getPath();
        return Identifier.fromNamespaceAndPath(ExoticWorlds.MODID,
                BARE_TORUS_PRESETS.contains(path) ? TORUS_PRESET_PREFIX + path : path);
    }

    public static <T> @Nullable ResourceKey<T> twinOf(@Nullable ResourceKey<T> key) {
        Identifier twin = key == null ? null : twinOf(key.identifier());
        return twin == null ? null : ResourceKey.create(key.registryKey(), twin);
    }

    public static @Nullable String twinKeyOf(String key) {
        Identifier twin = twinOf(Identifier.tryParse(key));
        return twin == null ? null : twin.toString();
    }

    public static String formerKey(String key) {
        if (!key.startsWith(CURRENT_KEY_PREFIX)) {
            throw new IllegalArgumentException("Not a key of ours: " + key);
        }

        return KEY_PREFIX + key.substring(CURRENT_KEY_PREFIX.length());
    }

    public static Optional<CompoundTag> compound(CompoundTag tag, String key) {
        Optional<CompoundTag> current = tag.getCompound(key);
        return current.isPresent() ? current : tag.getCompound(formerKey(key));
    }

    private FormerNamespace() {
    }
}
