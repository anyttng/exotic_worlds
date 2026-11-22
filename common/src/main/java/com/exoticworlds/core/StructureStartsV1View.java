package com.exoticworlds.core;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.gen.StructureStarts;
import com.exoticworlds.api.v1.option.GenerationOptions;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

final class StructureStartsV1View implements StructureStarts {
    private final com.exoticworlds.api.v2.gen.StructureStarts set;
    private final ToroidalShape shape;
    private @Nullable List<Pick> picks;

    StructureStartsV1View(com.exoticworlds.api.v2.gen.StructureStarts set, ToroidalShape shape) {
        this.set = set;
        this.shape = shape;
    }

    @Override
    public ToroidalShape shape() {
        return this.shape;
    }

    @Override
    public GenerationOptions options() {
        return this.set.options();
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
