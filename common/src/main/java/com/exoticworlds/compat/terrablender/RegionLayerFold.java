package com.exoticworlds.compat.terrablender;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.FoldCompression;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.QuartPos;

public record RegionLayerFold(WorldFold transformer, int topDepth, double factor, LayerAxis x, LayerAxis z,
        int skew) {
    public static RegionLayerFold of(WorldFold transformer, int topDepth, double factor) {
        TranslationLattice lattice = transformer.blockLattice();
        return new RegionLayerFold(transformer, topDepth, factor,
                LayerAxis.of(lattice.x(), topDepth, factor),
                LayerAxis.of(lattice.z(), topDepth, factor),
                QuartPos.fromBlock(lattice.skew()));
    }

    static RegionLayerFold resolve(@Nullable RegionLayerFold cached, WorldFold transformer, int topDepth) {
        return cached != null && cached.transformer == transformer && cached.topDepth == topDepth
                ? cached
                : of(transformer, topDepth, FoldCompression.of(transformer));
    }

    public int foldX(int depth, int coordX, int coordZ) {
        return depth == this.topDepth
                ? this.x.toVirtual(sheared(coordX, coordZ))
                : this.x.fold(coordX, this.topDepth - depth);
    }

    public int foldZ(int depth, int coordZ) {
        return depth == this.topDepth
                ? this.z.toVirtual(coordZ)
                : this.z.fold(coordZ, this.topDepth - depth);
    }

    private int sheared(int quartX, int quartZ) {
        if (this.skew == TranslationLattice.NO_SKEW) {
            return quartX;
        }

        long offset = (long) quartZ - this.z.min();
        long laps = Math.floorDiv(offset, this.z.lap());
        long row = offset - laps * this.z.lap();
        return (int) (quartX - laps * this.skew - Math.floorDiv(row * this.skew, this.z.lap()));
    }

    public record LayerAxis(boolean loops, double factor, int lap, int min, int scaledLap, int origin, int interior,
            int strip, int stripCells, int cell, int virtualLap) {
        static LayerAxis unbounded(double factor) {
            return new LayerAxis(false, factor, 0, 0, 0, 0, 0, 0, 0, 1, 0);
        }

        static LayerAxis of(WrapDomain blocks, int topDepth, double factor) {
            if (!blocks.loops()) {
                return unbounded(factor);
            }

            int cell = 1 << topDepth;
            int lap = QuartPos.fromBlock(blocks.domainLength);
            int min = QuartPos.fromBlock(blocks.lowerBound);
            int scaledLap = Math.max(1, (int) Math.round(lap * factor));
            int origin = Math.ceilDiv(min, cell) * cell;
            int interior = Math.max(0, Math.floorDiv(min + scaledLap, cell) * cell - origin);
            int strip = scaledLap - interior;
            int stripCells = strip == 0 ? 0 : Math.max(interior == 0 ? 1 : 0, (strip + cell / 2) / cell);
            int virtualLap = interior + stripCells * cell;
            return new LayerAxis(true, factor, lap, min, scaledLap, origin, interior, strip, stripCells, cell,
                    virtualLap);
        }

        int toVirtual(int quart) {
            if (!this.loops) {
                return (int) Math.floor(quart * this.factor);
            }

            int scaled = this.min + (int) ((long) Math.floorMod(quart - this.min, this.lap) * this.scaledLap / this.lap);
            int offset = Math.floorMod(scaled - this.origin, this.scaledLap);
            if (this.strip == 0) {
                return this.origin + offset;
            }

            int stretchedStart = this.stripCells > 0 ? this.interior : this.interior - this.cell;
            if (offset < stretchedStart) {
                return this.origin + offset;
            }

            long rawSpan = (long) this.scaledLap - stretchedStart;
            long virtualSpan = (long) this.virtualLap - stretchedStart;
            return this.origin + stretchedStart + (int) ((offset - stretchedStart) * virtualSpan / rawSpan);
        }

        int fold(int coord, int shift) {
            if (!this.loops) {
                return coord;
            }

            int start = this.origin >> shift;
            return start + Math.floorMod(coord - start, this.virtualLap >> shift);
        }
    }
}
