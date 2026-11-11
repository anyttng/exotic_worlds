package com.exoticworlds.engine.noise;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.ThreadScope;

import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

public final class CarverRecording {
    private static final ThreadScope<CarverRecording> SCOPE = new ThreadScope<>();

    private static final int COLUMN_BITS = 8;
    private static final int X_BITS = 4;

    private final IdentityHashMap<WorldCarver, BitSet> carved = new IdentityHashMap<>();

    static <R> R recording(CarverRecording recording, Supplier<R> body) {
        return SCOPE.with(recording, body);
    }

    public static @Nullable CarverOutput outputFor(WorldCarver carver, CarverOutput output) {
        CarverRecording recording = SCOPE.current();
        if (recording == null) {
            return null;
        }

        BitSet bits = recording.carved.computeIfAbsent(carver, _ -> new BitSet());
        return new CarverOutput() {
            @Override
            public void carve(int x, int y, int z) {
                bits.set(((y - output.minY()) << COLUMN_BITS) | (x << X_BITS) | z);
                output.carve(x, y, z);
            }

            @Override
            public int minY() {
                return output.minY();
            }

            @Override
            public int maxY() {
                return output.maxY();
            }
        };
    }

    IdentityHashMap<WorldCarver, BitSet> carved() {
        return this.carved;
    }
}
