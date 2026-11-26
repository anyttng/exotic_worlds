package com.exoticworlds.bridge.v2.feature;

import java.util.List;

import com.exoticworlds.api.v2.feature.StructureStartsV2;
import com.exoticworlds.core.gen.StructureStartSet;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

final class StructureStartsView extends MomentView implements StructureStartsV2 {
    private final StructureStartSet set;

    StructureStartsView(StructureStartSet set) {
        super(set.getFold(), set.getDimension(), set.getOptions());
        this.set = set;
    }

    @Override
    public Holder<StructureSet> getStructureSet() {
        return this.set.getStructureSet();
    }

    @Override
    public List<Pick> getPicks() {
        return this.set.getPicks().stream().map(pick -> new Pick(pick.chunk(), pick.structure())).toList();
    }

    @Override
    public boolean canStart(Holder<Structure> structure, ChunkPos chunk) {
        return this.set.canStart(structure, chunk);
    }
}
