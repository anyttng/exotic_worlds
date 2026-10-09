package com.exoticworlds.compat.scalablelux;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;

public final class LightLockFolds {
    private static final Map<Integer, Owner> OWNERS = new ConcurrentHashMap<>();

    public static void register(int ownerTag, Object lightEngine, TransformerSource source) {
        OWNERS.put(ownerTag, new Owner(lightEngine, source));
    }

    public static void release(Object lightEngine) {
        OWNERS.values().removeIf(owner -> owner.lightEngine() == lightEngine);
    }

    public static long foldKey(int ownerTag, long chunkKey) {
        Owner owner = OWNERS.get(ownerTag);
        return foldKey(owner != null ? owner.source().toroidal$wrappedTransformer() : null, chunkKey);
    }

    static long foldKey(@Nullable WorldFold fold, long chunkKey) {
        return fold != null ? fold.foldChunkKey(chunkKey) : chunkKey;
    }

    @SuppressWarnings("NonApiType")
    public static <T> ArrayList<T> distinct(ArrayList<T> tokens) {
        LinkedHashSet<T> unique = new LinkedHashSet<>(tokens);
        return unique.size() == tokens.size() ? tokens : new ArrayList<>(unique);
    }

    private record Owner(Object lightEngine, TransformerSource source) {
    }

    private LightLockFolds() {
    }
}
