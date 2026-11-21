package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class SimplexSlot {
    private record Held(SimplexNoise noise, SimplexField field) {
    }

    private volatile @Nullable Held held;

    public SimplexField field(WorldFold fold, double scale, SimplexNoise noise) {
        Held current = this.held;
        if (current != null && current.noise() == noise && current.field().fold() == fold
                && Double.compare(current.field().scale(), scale) == 0) {
            return current.field();
        }

        SimplexField built = FoldedSamplers.simplexField(fold, scale, noise);
        this.held = new Held(noise, built);
        return built;
    }

    public float sample(SimplexNoise noise, WorldFold fold, double scale, double x, double z) {
        return (float) field(fold, scale, noise).sample(noise, x, z);
    }

    public static final class Stack {
        private static final String NOT_SIMPLEX_LEAF = "A simplex stack samples simplex leaves only, got ";

        @SuppressWarnings("ArrayRecordComponent")
        private record Layers(WorldFold fold, double scale, NoiseStack stack, SimplexNoise[] leaves,
                SimplexField[] fields) {
        }

        private volatile @Nullable Layers held;

        public float sample(WorldFold fold, double scale, NoiseStack stack, double x, double z) {
            Layers layers = layers(fold, scale, stack);
            float value = 0.0F;
            for (int i = 0; i < layers.fields().length; i++) {
                float layer = (float) layers.fields()[i].sample(layers.leaves()[i], x, z);
                value += stack.layers[i].amplitude() * layer;
            }

            return value;
        }

        private Layers layers(WorldFold fold, double scale, NoiseStack stack) {
            Layers current = this.held;
            if (current != null && current.fold() == fold && Double.compare(current.scale(), scale) == 0
                    && current.stack() == stack) {
                return current;
            }

            SimplexNoise[] leaves = new SimplexNoise[stack.layers.length];
            SimplexField[] fields = new SimplexField[leaves.length];
            for (int i = 0; i < leaves.length; i++) {
                NoiseStack.Layer layer = stack.layers[i];
                if (!(layer.noise() instanceof SimplexNoise leaf)) {
                    throw new IllegalArgumentException(NOT_SIMPLEX_LEAF + layer.noise().getClass().getName());
                }

                leaves[i] = leaf;
                fields[i] = FoldedSamplers.simplexField(fold, scale * layer.frequency(), leaf);
            }

            Layers built = new Layers(fold, scale, stack, leaves, fields);
            this.held = built;
            return built;
        }
    }
}
