package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.world.level.Level;

public final class DhShapes {
    private static final DhLatticeMemo CLIENT = new DhLatticeMemo();

    public static @Nullable DhLattice of(IDhLevel level) {
        return level == null
                ? null
                : ((DhLevelLattice) level).exotic_worlds$getLattices().latticeOf(getFold(toMcLevel(level.getLevelWrapper())));
    }

    public static @Nullable WorldFold getServerFold(IDhLevel level) {
        Level mcLevel = toMcLevel(level.getLevelWrapper());
        return mcLevel != null && !mcLevel.isClientSide() ? WorldLoopAttachments.getWrappedFold(mcLevel) : null;
    }

    public static @Nullable DhLattice getClientFrame(ILevelWrapper wrapper) {
        Level mcLevel = toMcLevel(wrapper);
        return mcLevel != null && mcLevel.isClientSide() ? getClientLattice(mcLevel) : null;
    }

    static @Nullable DhLattice getClientLattice(@Nullable Level mcLevel) {
        return CLIENT.latticeOf(getFold(mcLevel));
    }

    private static @Nullable Level toMcLevel(IDhApiLevelWrapper wrapper) {
        return wrapper != null && wrapper.getWrappedMcObject() instanceof Level mcLevel ? mcLevel : null;
    }

    private static @Nullable WorldFold getFold(@Nullable Level mcLevel) {
        if (mcLevel == null) {
            return null;
        }

        return mcLevel.isClientSide()
                ? WorldLoopAttachments.getWrappedClientBoundsFold(mcLevel)
                : WorldLoopAttachments.getWrappedFold(mcLevel);
    }

    private DhShapes() {
    }
}
