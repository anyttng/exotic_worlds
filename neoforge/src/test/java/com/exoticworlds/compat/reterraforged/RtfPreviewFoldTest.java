package com.exoticworlds.compat.reterraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import raccoonman.reterraforged.data.worldgen.preset.PresetNoiseData;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

class RtfPreviewFoldTest {
    private static final int CHUNKS = 16;

    private static final int SEED = 1337;

    private static final int TILE_SIZE = 4;

    private static final int TILE_BORDER = 0;

    private static final int BATCH_COUNT = 1;

    private static final float X = 1234.0F;

    private static final float Z = -567.0F;

    private static final Preset PRESET = Presets.makeRTFDefault();

    @Test
    void aPreviewBuiltUnderTheChosenShapeCarriesItsFold() {
        WorldFold fold = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-CHUNKS, CHUNKS, -CHUNKS, CHUNKS)));
        GeneratorContext context = GenerationTransformerContext.withRouterBuild(fold, RtfPreviewFoldTest::preview);

        assertSame(fold, ((LapCarrier) (Object) context.generator.getHeightmap()).toroidal$fold());
    }

    @Test
    void aPreviewOfTheChosenShapeRepeatsAtTheLap() {
        WorldFold fold = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-CHUNKS, CHUNKS, -CHUNKS, CHUNKS)));
        Heightmap heightmap = GenerationTransformerContext.withRouterBuild(fold, RtfPreviewFoldTest::preview)
                .generator.getHeightmap();
        int xLap = fold.blockLattice().x().domainLength;
        int zLap = fold.blockLattice().z().domainLength;

        Cell base = sample(heightmap, X, Z);
        for (Cell copy : new Cell[] {sample(heightmap, X + xLap, Z), sample(heightmap, X, Z + zLap)}) {
            assertEquals(base.height, copy.height);
            assertEquals(base.temperature, copy.temperature);
        }
    }

    @Test
    void aPreviewBuiltWithNoShapeCarriesNoFold() {
        GeneratorContext context = GenerationTransformerContext.withRouterBuild(null, RtfPreviewFoldTest::preview);

        assertNull(((LapCarrier) (Object) context.generator.getHeightmap()).toroidal$fold());
    }

    private static GeneratorContext preview() {
        return GeneratorContext.makeUncached(PRESET, noises(), SEED, TILE_SIZE, TILE_BORDER, BATCH_COUNT);
    }

    private static HolderGetter<Noise> noises() {
        return new RegistrySetBuilder()
                .add(RTFRegistries.NOISE, context -> PresetNoiseData.bootstrap(PRESET, context))
                .build(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY))
                .lookupOrThrow(RTFRegistries.NOISE);
    }

    private static Cell sample(Heightmap heightmap, float x, float z) {
        Cell cell = new Cell();
        heightmap.apply(cell, x, z, true);
        return cell;
    }
}
