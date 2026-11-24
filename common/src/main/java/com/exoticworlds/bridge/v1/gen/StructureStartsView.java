package com.exoticworlds.bridge.v1.gen;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.gen.StructureStarts;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.core.gen.StructureStartSet;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

final class StructureStartsView implements StructureStarts {
    private final StructureStartSet set;
    private final ToroidalShape shape;
    private @Nullable List<Pick> picks;

    StructureStartsView(StructureStartSet set, ToroidalShape shape) {
        this.set = set;
        this.shape = shape;
    }

    @Override
    public ToroidalShape shape() {
        return this.shape;
    }

    @Override
    public GenerationOptions options() {
        return OptionBridge.toV1(this.set.getOptions());
    }

    @Override
    public Holder<StructureSet> structureSet() {
        return this.set.getStructureSet();
    }

    @Override
    public List<Pick> picks() {
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
