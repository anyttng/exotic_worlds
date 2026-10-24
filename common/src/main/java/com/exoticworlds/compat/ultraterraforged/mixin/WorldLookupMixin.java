package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.ultraterraforged.LapCarrier;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.WorldLookup;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

@Mixin(value = WorldLookup.class, remap = false)
public abstract class WorldLookupMixin {
    @Shadow
    private Levels levels;

    @WrapMethod(method = "apply(Lcom/pandaismyname1/ultraterraforged/world/worldgen/cell/Cell;IIZ)Z")
    private boolean exotic_worlds$foldLookup(Cell cell, int x, int z, boolean load, Operation<Boolean> original) {
        WorldFold fold = LapCarrier.foldOf(this.levels);
        if (fold == null) {
            return original.call(cell, x, z, load);
        }

        TranslationLattice lattice = fold.blockLattice();
        return original.call(cell, lattice.foldX(x, z), lattice.foldZ(z), load);
    }

    @WrapMethod(method = "getGenerationHeight")
    private int exotic_worlds$foldChunk(int chunkX, int chunkZ, NoiseGeneratorSettings settings, boolean load,
            Operation<Integer> original) {
        WorldFold fold = LapCarrier.foldOf(this.levels);
        if (fold == null) {
            return original.call(chunkX, chunkZ, settings, load);
        }

        TranslationLattice lattice = fold.chunkLattice();
        return original.call(lattice.foldX(chunkX, chunkZ), lattice.foldZ(chunkZ), settings, load);
    }
}
