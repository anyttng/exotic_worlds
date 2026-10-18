package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.world.level.Level;

public final class DhShapes {
    private static final DhLatticeMemo CLIENT = new DhLatticeMemo();

    public static @Nullable DhLattice of(IDhLevel level) {
        return level == null
                ? null
                : ((DhLevelLattice) level).exotic_worlds$lattices().latticeOf(foldOf(mcLevel(level.getLevelWrapper())));
    }

    public static @Nullable WorldFold serverFoldOf(IDhLevel level) {
        Level mcLevel = mcLevel(level.getLevelWrapper());
        return mcLevel != null && !mcLevel.isClientSide() ? WorldLoopAttachments.wrappedTransformerOf(mcLevel) : null;
    }

    public static @Nullable DhLattice clientFrame(ILevelWrapper wrapper) {
        Level mcLevel = mcLevel(wrapper);
        return mcLevel != null && mcLevel.isClientSide() ? clientLattice(mcLevel) : null;
    }

    static @Nullable DhLattice clientLattice(@Nullable Level mcLevel) {
        return CLIENT.latticeOf(foldOf(mcLevel));
    }

    private static @Nullable Level mcLevel(IDhApiLevelWrapper wrapper) {
        return wrapper != null && wrapper.getWrappedMcObject() instanceof Level mcLevel ? mcLevel : null;
    }

    private static @Nullable WorldFold foldOf(@Nullable Level mcLevel) {
        if (mcLevel == null) {
            return null;
        }

        return mcLevel.isClientSide()
                ? WorldLoopAttachments.wrappedClientBoundsTransformerOf(mcLevel)
                : WorldLoopAttachments.wrappedTransformerOf(mcLevel);
    }

    private DhShapes() {
    }
}
