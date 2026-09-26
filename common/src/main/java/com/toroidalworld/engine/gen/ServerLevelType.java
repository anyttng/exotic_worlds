package com.toroidalworld.engine.gen;

import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import com.toroidalworld.ToroidalWorld;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public final class ServerLevelType {
    private static final String LEVEL_TYPE_KEY = "level-type";

    public static WorldDimensions keepChosenShape(WorldDimensions created, Properties properties,
            HolderLookup.Provider registries) {
        Optional<Holder.Reference<WorldPreset>> chosen = chosenPreset(properties, registries);
        if (chosen.isEmpty()) {
            return created;
        }

        WorldDimensions kept = ShapedDimensions.keepChosenShape(created, chosen.get().value().createWorldDimensions());
        if (kept != created) {
            ToroidalWorld.LOGGER.warn("[server-preset] restored level_type={} reason=replaced_before_creation",
                    chosen.get().key().identifier());
        }

        return kept;
    }

    private static Optional<Holder.Reference<WorldPreset>> chosenPreset(Properties properties,
            HolderLookup.Provider registries) {
        String levelType = properties.getProperty(LEVEL_TYPE_KEY);
        Identifier id = levelType == null ? null : Identifier.tryParse(levelType.toLowerCase(Locale.ROOT));
        return id == null
                ? Optional.empty()
                : registries.lookupOrThrow(Registries.WORLD_PRESET).get(ResourceKey.create(Registries.WORLD_PRESET, id));
    }

    private ServerLevelType() {
    }
}
