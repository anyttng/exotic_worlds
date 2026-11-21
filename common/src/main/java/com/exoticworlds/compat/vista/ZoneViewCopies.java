package com.exoticworlds.compat.vista;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.net.TranslationContext;
import com.exoticworlds.engine.seam.HeldArea;

import net.minecraft.world.level.ChunkPos;

public final class ZoneViewCopies {
    public static @Nullable ChunkPos owedZoneCopy(TranslationContext context, ChunkPos chunk) {
        ChunkPos viewCopy = context.viewSeat(chunk);
        for (HeldArea area : context.clientPosition().heldAreas()) {
            ChunkPos zoneCopy = area.copyOf(context.fold(), chunk);
            if (zoneCopy != null && !zoneCopy.equals(viewCopy)) {
                return zoneCopy;
            }
        }

        return null;
    }

    private ZoneViewCopies() {
    }
}
