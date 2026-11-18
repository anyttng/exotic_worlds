package com.exoticworlds.compat.create;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public final class LappedKeys extends AbstractSet<BlockPos> {
    private final WorldFold fold;
    private final Map<BlockPos, BlockPos> claims = new HashMap<>();

    private boolean lapped;

    public static Set<BlockPos> set(@Nullable BlockGetter reader, Set<BlockPos> built) {
        WorldFold fold = CanonicalPositionKeys.wrappedFoldOf(reader);
        return fold == null ? built : new LappedKeys(fold);
    }

    public static boolean claimedByACopy(Set<BlockPos> visited, BlockPos pos) {
        return visited instanceof LappedKeys keys && keys.contains(pos);
    }

    LappedKeys(WorldFold fold) {
        this.fold = fold;
    }

    public boolean lapped() {
        return lapped;
    }

    @Override
    public boolean add(BlockPos pos) {
        BlockPos key = fold.fold(pos);
        BlockPos claimed = claims.putIfAbsent(key, pos);
        if (claimed == null) {
            return true;
        }

        witness(pos, claimed);
        return false;
    }

    @Override
    public boolean contains(Object candidate) {
        if (!(candidate instanceof BlockPos pos)) {
            return false;
        }

        BlockPos claimed = claims.get(fold.fold(pos));
        if (claimed == null) {
            return false;
        }

        witness(pos, claimed);
        return true;
    }

    @Override
    public boolean remove(Object candidate) {
        return candidate instanceof BlockPos pos && claims.remove(fold.fold(pos)) != null;
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return claims.values().iterator();
    }

    @Override
    public int size() {
        return claims.size();
    }

    @Override
    public void clear() {
        claims.clear();
    }

    private void witness(BlockPos arrived, BlockPos claimed) {
        if (!arrived.equals(claimed)) {
            lapped = true;
        }
    }
}
