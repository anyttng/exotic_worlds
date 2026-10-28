package com.exoticworlds.compat.sable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class SableColliderCopies {
    public static List<SectionPos> held(WorldFold fold, SectionPos physical, Map<SectionPos, ?> held) {
        if (held.isEmpty()) {
            return List.of();
        }

        List<SectionPos> copies = new ArrayList<>();
        for (ChunkPos alias : SableChunkKeys.rawAliases(fold, physical.chunk())) {
            SectionPos copy = SectionPos.of(alias, physical.y());
            if (held.containsKey(copy)) {
                copies.add(copy);
            }
        }

        return copies;
    }

    private SableColliderCopies() {
    }
}
