package com.exoticworlds.engine.noise;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.exoticworlds.core.gen.DensityNoises;
import com.exoticworlds.core.gen.RouterField;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public record NoiseRouterFields(Map<Holder<NormalNoise.NoiseParameters>, Set<RouterField>> byNoise) {
    public static final NoiseRouterFields NONE = new NoiseRouterFields(Map.of());

    public static NoiseRouterFields of(NoiseRouter router) {
        Map<Holder<NormalNoise.NoiseParameters>, Set<RouterField>> byNoise = new HashMap<>();
        for (RouterField field : RouterField.values()) {
            for (DensityFunction.NoiseHolder noise : DensityNoises.noisesUnder(field.getFunction(router))) {
                byNoise.computeIfAbsent(noise.noiseData(), key -> EnumSet.noneOf(RouterField.class)).add(field);
            }
        }

        byNoise.replaceAll((noise, fields) -> Set.copyOf(fields));
        return new NoiseRouterFields(Map.copyOf(byNoise));
    }

    public Set<RouterField> getFields(Holder<NormalNoise.NoiseParameters> noise) {
        return this.byNoise.getOrDefault(noise, Set.of());
    }
}
