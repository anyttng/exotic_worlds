package com.exoticworlds.compat.wover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Locale;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker;
import org.betterx.wover.generator.api.biomesource.WoverBiomePicker.PickableBiome;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

class EdgeBiomesTest {
    private static final int PALETTE = 16;

    private static final int EDGE_KEY = PALETTE;

    private static final int EDGE_SIZE = 8;

    private static final int SEED = 0x900;

    private static final float HEX_SCALE = 32.0F;

    private static final int SQUARE_SIZE = 64;

    private static final int LAP_CHUNKS = 256;

    private static final int WINDOW_BLOCKS = 4096;

    private static final int STEP_BLOCKS = 8;

    private static final double FACTOR = 4.0;

    private static final double NO_COMPRESSION = 1.0;

    private static final double SHARE_TOLERANCE = 0.15;

    private static final String EDGE_FIELD = "edge";

    private static final String EDGE_SIZE_FIELD = "edgeSize";

    private static final String PALETTE_NAMESPACE = "cject";

    private static final String PALETTE_PATH = "edge_palette_";

    private interface MapMaker {
        LapMap<PickableBiome> make(WorldFold fold, double factor, LapPicker<PickableBiome> picker);
    }

    private record Palette(PickableBiome[] biomes, PickableBiome edged, PickableBiome edge) {
        static Palette create() {
            PickableBiome[] biomes = new PickableBiome[PALETTE];
            for (int index = 0; index < PALETTE; index++) {
                biomes[index] = biome(index);
            }

            PickableBiome edge = biome(EDGE_KEY);
            setField(biomes[0], EDGE_FIELD, edge);
            setField(biomes[0], EDGE_SIZE_FIELD, EDGE_SIZE);
            return new Palette(biomes, biomes[0], edge);
        }

        LapPicker<PickableBiome> picker() {
            return new LapPicker<>(random -> this.biomes[random.nextInt(PALETTE)], (biome, random) -> biome);
        }

        private static PickableBiome biome(int index) {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME,
                    Identifier.fromNamespaceAndPath(PALETTE_NAMESPACE, PALETTE_PATH + index));
            return new WoverBiomePicker((HolderGetter<Biome>) null, key).fallbackBiome;
        }

        private static void setField(PickableBiome biome, String name, Object value) {
            try {
                Field field = PickableBiome.class.getDeclaredField(name);
                field.setAccessible(true);
                field.set(biome, value);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Test
    void aHexEdgeRingShrinksWithItsRegionsUnderCompactBiomes() {
        assertEdgeShareKept((fold, factor, picker) -> new HexLapMap<>(fold, HEX_SCALE, factor, SEED, picker),
                EdgeBiomes::hex);
    }

    @Test
    void aSquareEdgeRingShrinksWithItsRegionsUnderCompactBiomes() {
        assertEdgeShareKept((fold, factor, picker) -> new SquareLapMap<>(fold, SQUARE_SIZE, factor, SEED, picker),
                EdgeBiomes::square);
    }

    private static void assertEdgeShareKept(MapMaker maker, EdgeReader reader) {
        Palette palette = Palette.create();
        WorldFold torus = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(LAP_CHUNKS)));
        double full = edgeShare(maker.make(torus, NO_COMPRESSION, palette.picker()), reader, palette,
                NO_COMPRESSION);
        double compact = edgeShare(maker.make(torus, FACTOR, palette.picker()), reader, palette, FACTOR);
        String readings = String.format(Locale.ROOT, "edge share of the edged biome: %.3f at factor 1, %.3f at "
                + "factor %.0f", full, compact, FACTOR);
        assertTrue(full > 0.0, readings);
        assertEquals(1.0, compact / full, SHARE_TOLERANCE, readings);
    }

    private interface EdgeReader {
        PickableBiome at(LapMap<PickableBiome> map, double x, double z);
    }

    private static double edgeShare(LapMap<PickableBiome> map, EdgeReader reader, Palette palette, double factor) {
        double window = WINDOW_BLOCKS / factor;
        double step = STEP_BLOCKS / factor;
        long edged = 0;
        long edges = 0;
        for (double x = -window / 2.0; x < window / 2.0; x += step) {
            for (double z = -window / 2.0; z < window / 2.0; z += step) {
                if (map.biomeAt(x, z) != palette.edged()) {
                    continue;
                }

                edged++;
                if (reader.at(map, x, z) == palette.edge()) {
                    edges++;
                }
            }
        }

        return (double) edges / edged;
    }
}
