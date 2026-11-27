package com.exoticworlds.engine.noise;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.exoticworlds.core.gen.DensityNoises;
import com.exoticworlds.core.gen.RouterField;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public record NoiseRouterFields(Map<Holder<NormalNoise>, Set<RouterField>> byNoise) {
    public static final NoiseRouterFields NONE = new NoiseRouterFields(Map.of());

    public static NoiseRouterFields of(NoiseRouter router) {
        Map<Holder<NormalNoise>, Set<RouterField>> byNoise = new HashMap<>();
        for (RouterField field : RouterField.values()) {
            for (Holder<NormalNoise> noise : DensityNoises.noisesUnder(field.getFunction(router))) {
                byNoise.computeIfAbsent(noise, key -> EnumSet.noneOf(RouterField.class)).add(field);
            }
        }

        byNoise.replaceAll((noise, fields) -> Set.copyOf(fields));
        return new NoiseRouterFields(Map.copyOf(byNoise));
    }

    public Set<RouterField> getFields(Holder<NormalNoise> noise) {
        return this.byNoise.getOrDefault(noise, Set.of());
    }
}
