package com.exoticworlds.bridge.v2.gen;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.gen.StructureStarts;
import com.exoticworlds.api.v2.geometry.WorldGeometry;
import com.exoticworlds.api.v2.option.ShapeOptions;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.core.StructureStartSet;
import com.exoticworlds.shape.ShapeOptionsAdapter;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

final class StructureStartsView implements StructureStarts {
    private final StructureStartSet set;
    private @Nullable List<Pick> picks;

    StructureStartsView(StructureStartSet set) {
        this.set = set;
    }

    @Override
    public WorldGeometry geometry() {
        return GeometryBridge.of(this.set.fold());
    }

    @Override
    public ShapeOptions options() {
        return ShapeOptionsAdapter.shapeOptions(this.set.options());
    }

    @Override
    public Holder<StructureSet> structureSet() {
        return this.set.structureSet();
    }

    @Override
    public List<Pick> picks() {
        if (this.picks == null) {
            this.picks = this.set.picks().stream()
                    .map(pick -> new Pick(pick.chunk(), pick.structure()))
                    .toList();
        }

        return this.picks;
    }

    @Override
    public boolean canStart(Holder<Structure> structure, ChunkPos chunk) {
        return this.set.canStart(structure, chunk);
    }
}
