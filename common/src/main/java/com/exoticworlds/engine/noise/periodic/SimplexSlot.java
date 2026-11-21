package com.exoticworlds.engine.noise.periodic;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.levelgen.synth.NoiseStack;

public final class SimplexSlot {
    private volatile @Nullable SimplexLattice held;

    public SimplexLattice lattice(WorldFold fold, double scale) {
        SimplexLattice current = this.held;
        if (current != null && current.fold() == fold && Double.compare(current.scale(), scale) == 0) {
            return current;
        }

        SimplexLattice built = SimplexLattice.of(fold, scale);
        this.held = built;
        return built;
    }

    public static final class Stack {
        @SuppressWarnings("ArrayRecordComponent")
        private record Layers(WorldFold fold, double scale, NoiseStack stack, SimplexLattice[] lattices) {
        }

        private volatile @Nullable Layers held;

        SimplexLattice[] lattices(WorldFold fold, double scale, NoiseStack stack) {
            Layers current = this.held;
            if (current != null && current.fold() == fold && Double.compare(current.scale(), scale) == 0
                    && current.stack() == stack) {
                return current.lattices();
            }

            SimplexLattice[] lattices = new SimplexLattice[stack.layers.length];
            for (int i = 0; i < lattices.length; i++) {
                lattices[i] = SimplexLattice.of(fold, scale * stack.layers[i].frequency());
            }

            this.held = new Layers(fold, scale, stack, lattices);
            return lattices;
        }
    }
}
