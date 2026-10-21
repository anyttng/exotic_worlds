package com.exoticworlds.compat.ultimatemapatlases;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.ClientShapes;
import com.google.common.base.Splitter;

import net.minecraft.core.BlockPos;

public final class AtlasBlockMarkerKey {
    private static final String PREFIX = "block_";
    private static final Splitter SEPARATOR = Splitter.on(", ");
    private static final int COORDINATES = 3;

    public static String folded(@Nullable ToroidalShape shape, String key) {
        BlockPos pos = posOf(key);
        if (pos == null) {
            return key;
        }

        BlockPos folded = ClientShapes.fold(shape, pos);
        return folded.equals(pos) ? key : PREFIX + folded.toShortString();
    }

    private static @Nullable BlockPos posOf(String key) {
        if (!key.startsWith(PREFIX)) {
            return null;
        }

        List<String> parts = SEPARATOR.splitToList(key.substring(PREFIX.length()));
        if (parts.size() != COORDINATES) {
            return null;
        }

        try {
            return new BlockPos(Integer.parseInt(parts.get(0)), Integer.parseInt(parts.get(1)),
                    Integer.parseInt(parts.get(2)));
        } catch (NumberFormatException malformed) {
            return null;
        }
    }

    private AtlasBlockMarkerKey() {
    }
}
