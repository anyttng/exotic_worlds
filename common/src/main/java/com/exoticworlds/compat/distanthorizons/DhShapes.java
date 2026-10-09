package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.world.level.Level;

public final class DhShapes {
    public static @Nullable DhLattice of(IDhLevel level) {
        return level == null ? null : of(level.getLevelWrapper());
    }

    public static @Nullable DhLattice of(IDhApiLevelWrapper wrapper) {
        return latticeOf(mcLevel(wrapper));
    }

    public static @Nullable WorldFold serverFoldOf(IDhLevel level) {
        Level mcLevel = mcLevel(level.getLevelWrapper());
        return mcLevel != null && !mcLevel.isClientSide() ? WorldLoopAttachments.wrappedTransformerOf(mcLevel) : null;
    }

    public static @Nullable DhLattice clientFrame(ILevelWrapper wrapper) {
        Level mcLevel = mcLevel(wrapper);
        return mcLevel != null && mcLevel.isClientSide() ? latticeOf(mcLevel) : null;
    }

    private static @Nullable Level mcLevel(IDhApiLevelWrapper wrapper) {
        return wrapper != null && wrapper.getWrappedMcObject() instanceof Level mcLevel ? mcLevel : null;
    }

    static @Nullable DhLattice latticeOf(@Nullable Level mcLevel) {
        if (mcLevel == null) {
            return null;
        }

        WorldFold fold = mcLevel.isClientSide()
                ? WorldLoopAttachments.wrappedClientBoundsTransformerOf(mcLevel)
                : WorldLoopAttachments.wrappedTransformerOf(mcLevel);
        if (fold == null) {
            return null;
        }

        DhLattice lattice = DhLattice.of(fold);
        DhProbes.keyPeriod(lattice, DhKeys.LEAF);
        return lattice;
    }

    private DhShapes() {
    }
}
