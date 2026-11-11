package com.exoticworlds.engine.noise;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.ThreadScope;

import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;

public final class CarverRecording {
    private static final ThreadScope<CarverRecording> SCOPE = new ThreadScope<>();

    private final IdentityHashMap<ConfiguredWorldCarver<?>, BitSet> carved = new IdentityHashMap<>();

    static <R> R recording(CarverRecording recording, Supplier<R> body) {
        return SCOPE.with(recording, body);
    }

    public static long @Nullable [] before(CarvingMask mask) {
        return SCOPE.current() != null ? mask.toArray() : null;
    }

    public static void after(ConfiguredWorldCarver<?> carver, CarvingMask mask, long @Nullable [] before) {
        CarverRecording recording = SCOPE.current();
        if (recording == null || before == null) {
            return;
        }

        BitSet added = BitSet.valueOf(mask.toArray());
        added.andNot(BitSet.valueOf(before));
        recording.carved.computeIfAbsent(carver, _ -> new BitSet()).or(added);
    }

    IdentityHashMap<ConfiguredWorldCarver<?>, BitSet> carved() {
        return this.carved;
    }
}
