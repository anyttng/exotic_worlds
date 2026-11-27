package com.exoticworlds.engine.gen.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidFold;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CuboidShape;
import com.exoticworlds.engine.noise.ClimateScanFixture;
import com.exoticworlds.engine.noise.ClimateScanFixture.WorldType;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasureStructure;

class CuboidRingPositionsTest {
    private static final long SEED = 1637L;

    private static final WorldType OVERWORLD = ClimateScanFixture.TYPES.getFirst();

    private static final int STRONGHOLD_DISTANCE_CHUNKS = 32;

    private static final int STRONGHOLD_SPREAD = 3;

    private static final int STRONGHOLD_COUNT = 128;

    // A flat state seeds its rings with 0, which puts the first ring position at chunk (-13, -106), past a corner here.
    private static final CuboidShape FIRST_RING_PAST_A_CORNER = new CuboidShape(16, 16, 4);

    private static final CuboidShape RINGS_ON_THE_NET = new CuboidShape(128, 128, 128);

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void aFirstRingPastACornerLeavesOneStrongholdOnTheNet() {
        WorldFold fold = foldOf(FIRST_RING_PAST_A_CORNER);

        List<ChunkPos> positions = strongholdRings(fold);

        assertEquals(1, positions.size(), positions.toString());
        assertFalse(fold.isOver(positions.getFirst()), positions.toString());
    }

    @Test
    void ringsLaidOverTheNetKeepEveryStrongholdOnIt() {
        WorldFold fold = foldOf(RINGS_ON_THE_NET);

        List<ChunkPos> positions = strongholdRings(fold);

        assertTrue(positions.size() > 1, positions.toString());
        for (ChunkPos position : positions) {
            assertFalse(fold.isOver(position), position + " of " + positions);
        }
    }

    private static WorldFold foldOf(CuboidShape shape) {
        return new CuboidFold(shape.atlas(), ShapeOptions.DEFAULT);
    }

    private static List<ChunkPos> strongholdRings(WorldFold fold) {
        BiomeSource biomeSource = ClimateScanFixture.biomeSource(OVERWORLD);
        HolderSet<Biome> biomes = HolderSet.direct(List.copyOf(biomeSource.possibleBiomes()));
        Holder<Structure> structure =
                Holder.direct(new BuriedTreasureStructure(new Structure.StructureSettings(biomes)));
        ConcentricRingsStructurePlacement placement = new ConcentricRingsStructurePlacement(
                STRONGHOLD_DISTANCE_CHUNKS, STRONGHOLD_SPREAD, STRONGHOLD_COUNT, biomes);
        RandomState randomState = ClimateScanFixture.randomState(OVERWORLD, fold, SEED);
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForFlat(randomState, SEED,
                new ChunkPos(0, 0), biomeSource, Stream.of(Holder.direct(new StructureSet(structure, placement))));
        ((FoldHolder) (Object) state).exotic_worlds$setFold(fold);
        return state.getRingPositionsFor(placement);
    }
}
