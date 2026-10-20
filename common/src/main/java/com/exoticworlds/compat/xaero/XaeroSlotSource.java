package com.exoticworlds.compat.xaero;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;

import net.minecraft.core.BlockPos;

record XaeroSlotSource(Kind kind, boolean folded, int blockX, int blockZ) {
    static final XaeroSlotSource RAW = new XaeroSlotSource(Kind.RAW, false, 0, 0);
    static final XaeroSlotSource HIDDEN = new XaeroSlotSource(Kind.HIDDEN, false, 0, 0);
    static final XaeroSlotSource FOLDED_HIDDEN = new XaeroSlotSource(Kind.HIDDEN, true, 0, 0);
    static final XaeroSlotSource ANY_CANONICAL = new XaeroSlotSource(Kind.ANY_CANONICAL, true, 0, 0);

    enum Kind {
        RAW,
        HIDDEN,
        ANY_CANONICAL,
        CANONICAL
    }

    static XaeroSlotSource of(@Nullable ToroidalShape shape, MapCopies mapCopies, boolean isCandidate,
            boolean glueable, boolean insideWorld, int viewBlockX, int viewBlockZ) {
        if (shape == null) {
            return isCandidate ? HIDDEN : RAW;
        }

        if (!glueable) {
            return !isCandidate && insideWorld ? RAW : ANY_CANONICAL;
        }

        BlockPos folded = XaeroWorldMapFold.foldBlock(shape, viewBlockX, viewBlockZ);
        if (folded.getX() == viewBlockX && folded.getZ() == viewBlockZ) {
            return isCandidate ? HIDDEN : RAW;
        }

        return mapCopies == MapCopies.SINGLE
                ? FOLDED_HIDDEN
                : new XaeroSlotSource(Kind.CANONICAL, true, folded.getX(), folded.getZ());
    }
}
