package com.exoticworlds.compat.terraforged;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

public final class LapOctaves {
    public static final String FRAME = "lapFrame";

    public static final String BLOCK_X = "lapBlockX";

    public static final String BLOCK_Z = "lapBlockZ";

    public static final String FREQUENCY = "lapFrequency";

    public static void open(LocalRef<RtfLap.@Nullable Frame> frame, LocalFloatRef blockX, LocalFloatRef blockZ,
            LocalDoubleRef frequency, float x, float z, double firstFrequency) {
        RtfLap.Frame bound = RtfLap.boundFrame();
        frame.set(bound);
        if (bound != null) {
            blockX.set(x);
            blockZ.set(z);
            frequency.set(firstFrequency);
        }
    }

    public static RtfLap.Frame.Scope next(RtfLap.Frame frame, LocalDoubleRef frequency, float lacunarity) {
        double octave = frequency.get();
        frequency.set(octave * lacunarity);
        return frame.octave(octave);
    }

    private LapOctaves() {
    }
}
