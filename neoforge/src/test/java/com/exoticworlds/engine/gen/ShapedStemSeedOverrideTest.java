package com.exoticworlds.engine.gen;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.shape.StartupRegistrations;
import static com.exoticworlds.engine.gen.BakeStampFixture.datapackRegistry;
import static com.exoticworlds.engine.gen.BakeStampFixture.dimensionType;
import static com.exoticworlds.engine.gen.BakeStampFixture.noiseGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.noiseSubclassGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.selected;
import static com.exoticworlds.engine.gen.BakeStampFixture.shapedGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.squareTorus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;


import java.util.Map;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;


import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

@Timeout(60)
class ShapedStemSeedOverrideTest {
    private static final double SAME_SCALE = 1.0;
    private static final long DECLARED_SEED = 0x5EED_0BE7L;
    private static final int WIDTH_CHUNKS = 64;
    private static final FlatShape SHAPE = squareTorus(WIDTH_CHUNKS);

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void withShapeKeepsTheSeedOverride() {
        LevelStem declared = declaredStem(noiseGenerator(worldgen));

        WorldDimensions shaped = ShapedDimensions.withShape(
                selected(Map.of(LevelStem.OVERWORLD, declared)), LevelStem.OVERWORLD, new CarriedShape(null, SHAPE,
                        ShapeOptions.DEFAULT));

        assertReshapedAlone(declared, shaped.get(LevelStem.OVERWORLD).orElseThrow());
    }

    @Test
    void stripShapesKeepsTheSeedOverride() {
        LevelStem declared = declaredStem(noiseGenerator(worldgen));

        WorldDimensions stripped = ShapedDimensions.stripShapes(selected(Map.of(LevelStem.OVERWORLD, declared)));

        assertReshapedAlone(declared, stripped.get(LevelStem.OVERWORLD).orElseThrow());
    }

    @Test
    void restoreStoredShapesKeepsTheSeedOverrideOnARebuiltStem() {
        LevelStem declared = declaredStem(noiseGenerator(worldgen));

        Registry<LevelStem> restored = restore(declared);

        assertReshapedAlone(declared, restored.getOptional(LevelStem.OVERWORLD).orElseThrow());
    }

    @Test
    void restoreStoredShapesKeepsTheSeedOverrideOnAStampedStem() {
        LevelStem declared = declaredStem(noiseSubclassGenerator(worldgen));

        Registry<LevelStem> restored = restore(declared);

        LevelStem restoredStem = restored.getOptional(LevelStem.OVERWORLD).orElseThrow();
        assertNotNull(GeneratorShapes.getCarriedShape(restoredStem.generator()), "the stem took no shape");
        assertSame(declared.type(), restoredStem.type());
        assertEquals(OptionalLong.of(DECLARED_SEED), restoredStem.seedOverride());
    }

    private static Registry<LevelStem> restore(LevelStem datapackStem) {
        WorldDimensions stored = selected(Map.of(LevelStem.OVERWORLD, plainStem(shapedGenerator(SHAPE))));
        return ShapedDimensions.restoreStoredShapes(stored,
                datapackRegistry(Map.of(LevelStem.OVERWORLD, datapackStem)));
    }

    private static void assertReshapedAlone(LevelStem declared, LevelStem reshaped) {
        assertNotSame(declared.generator(), reshaped.generator(), "the stem was not rebuilt");
        assertSame(declared.type(), reshaped.type());
        assertEquals(OptionalLong.of(DECLARED_SEED), reshaped.seedOverride());
    }

    private static LevelStem declaredStem(ChunkGenerator generator) {
        return new LevelStem(Holder.direct(dimensionType(SAME_SCALE)), generator, OptionalLong.of(DECLARED_SEED));
    }

    private static LevelStem plainStem(ChunkGenerator generator) {
        return new LevelStem(Holder.direct(dimensionType(SAME_SCALE)), generator);
    }
}
