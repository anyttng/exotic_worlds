package com.exoticworlds.bridge.v2.gen;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.gen.StructureStartsV2;
import com.exoticworlds.api.v2.geometry.WorldGeometryV2;
import com.exoticworlds.api.v2.option.ShapeOptionsV2;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.bridge.v2.option.ShapeOptionsAdapter;
import com.exoticworlds.core.gen.StructureStartSet;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

final class StructureStartsView implements StructureStartsV2 {
    private final StructureStartSet set;
    private @Nullable List<Pick> picks;

    StructureStartsView(StructureStartSet set) {
        this.set = set;
    }

    @Override
    public WorldGeometryV2 getGeometry() {
        return GeometryBridge.of(this.set.getFold());
    }

    @Override
    public ShapeOptionsV2 getOptions() {
        return ShapeOptionsAdapter.toV2(this.set.getOptions());
    }

    @Override
    public Holder<StructureSet> getStructureSet() {
        return this.set.getStructureSet();
    }

    @Override
    public List<Pick> getPicks() {
        if (this.picks == null) {
            this.picks = this.set.getPicks().stream()
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
