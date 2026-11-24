package com.exoticworlds.engine.noise;

import java.util.Arrays;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.periodic.SimplexLattice;

public final class SimplexSlots {
    @SuppressWarnings("ArrayRecordComponent")
    private record Octaves(double scale, WorldFold fold, SimplexLattice[] lattices) {
    }

    private volatile Octaves[] held = new Octaves[0];

    public SimplexLattice[] octaves(WorldFold fold, double scale, double highestInputFactor, int levels) {
        for (Octaves octaves : this.held) {
            if (Double.compare(octaves.scale(), scale) == 0 && octaves.fold() == fold) {
                return octaves.lattices();
            }
        }

        return this.built(fold, scale, highestInputFactor, levels);
    }

    private synchronized SimplexLattice[] built(WorldFold fold, double scale, double highestInputFactor, int levels) {
        SimplexLattice[] lattices = new SimplexLattice[levels];
        double factor = highestInputFactor;
        for (int i = 0; i < levels; i++) {
            lattices[i] = SimplexLattice.of(fold, scale * factor);
            factor /= 2.0;
        }

        Octaves[] current = this.held;
        int index = indexOf(current, scale);
        Octaves[] next = index < 0 ? Arrays.copyOf(current, current.length + 1) : current.clone();
        next[index < 0 ? current.length : index] = new Octaves(scale, fold, lattices);
        this.held = next;
        return lattices;
    }

    private static int indexOf(Octaves[] slots, double scale) {
        for (int i = 0; i < slots.length; i++) {
            if (Double.compare(slots[i].scale(), scale) == 0) {
                return i;
            }
        }

        return -1;
    }
}
