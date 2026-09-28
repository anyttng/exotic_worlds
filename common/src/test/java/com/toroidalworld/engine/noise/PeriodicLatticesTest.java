package com.toroidalworld.engine.noise;

import static com.toroidalworld.core.WorldFoldFixture.EVEN;
import static com.toroidalworld.core.WorldFoldFixture.ODD;
import static com.toroidalworld.core.WorldFoldFixture.UNEVEN;
import static com.toroidalworld.core.WorldFoldFixture.X_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.noise.GenerationTransformerContext.Context;

class PeriodicLatticesTest {
    private static final long SEED = 0x978L;

    private static final int SAMPLES = 2048;

    private static final double REACH_BLOCKS = 4096.0;

    private static final double STARVED_SCALE = 1.0 / 2048.0;

    private static final double LAYER_SCALE = 0.25;

    private static final double UNEVEN_SCALE = 1.17;

    private static final double DECLARED_SHARE = 1.0;

    private static final double CELL_DIVISOR = 4.0;

    private static final double[][] Y_PARAMS = {{0.0, 0.0}, {1.0, 2.0}, {0.25, -1.0}};

    private static final double UNDECLARED = GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE;

    private static final double UNDIVIDED = NoiseConstants.UNDIVIDED;

    private static final Key STARVED_TORUS = new Key(EVEN, SlotAxes.DEFAULT, STARVED_SCALE, DECLARED_SHARE, UNDIVIDED);

    private static final List<Key> KEYS = List.of(
            STARVED_TORUS,
            new Key(X_ONLY, SlotAxes.DEFAULT, STARVED_SCALE, DECLARED_SHARE, UNDIVIDED),
            new Key(EVEN, SlotAxes.DEFAULT, LAYER_SCALE, UNDECLARED, UNDIVIDED),
            new Key(EVEN, SlotAxes.DEFAULT, LAYER_SCALE, DECLARED_SHARE, UNDIVIDED),
            new Key(EVEN, SlotAxes.DEFAULT, LAYER_SCALE, UNDECLARED, CELL_DIVISOR),
            new Key(EVEN, DensityFunctionSlotAxes.SHIFT_B, LAYER_SCALE, UNDECLARED, UNDIVIDED),
            new Key(UNEVEN, SlotAxes.DEFAULT, UNEVEN_SCALE, UNDECLARED, UNDIVIDED));

    private record Key(WorldFold fold, SlotAxes axes, double scale, double verticalShare, double divisor) {
        <T> T under(Supplier<T> action) {
            Context context = GenerationTransformerContext.context();
            try (Context.BindingScope bindingScope = context.bind(this.fold, this.axes, this.scale, this.verticalShare);
                    Context.DivisorScope divisorScope = context.withDivisors(this.divisor, this.divisor)) {
                return action.get();
            }
        }

        Key withScale(double otherScale) {
            return new Key(this.fold, this.axes, otherScale, this.verticalShare, this.divisor);
        }
    }

    private record Noise(byte[] permutations, double xo, double yo, double zo) {
        static Noise of(Random random) {
            byte[] permutations = new byte[256];
            for (int i = 0; i < permutations.length; i++) {
                permutations[i] = (byte) i;
            }

            for (int i = permutations.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                byte swapped = permutations[i];
                permutations[i] = permutations[j];
                permutations[j] = swapped;
            }

            return new Noise(permutations, random.nextDouble() * 256.0, random.nextDouble() * 256.0,
                    random.nextDouble() * 256.0);
        }

        PeriodicLattice latticeIn(PeriodicLattices table, Key key) {
            return key.under(() -> table.latticeFor(this.permutations, this.xo, this.yo, this.zo, key.fold(),
                    GenerationTransformerContext.context()));
        }
    }

    @Test
    void aRepeatedKeyHandsBackTheSameLattice() {
        Noise noise = Noise.of(new Random(SEED));
        PeriodicLattices table = new PeriodicLattices();
        for (Key key : KEYS) {
            PeriodicLattice first = noise.latticeIn(table, key);
            assertSame(first, noise.latticeIn(table, key), () -> "rebuilt on an unchanged " + key);
        }
    }

    @Test
    void everyKeyFieldBuildsALatticeOfItsOwn() {
        Noise noise = Noise.of(new Random(SEED));
        PeriodicLattices table = new PeriodicLattices();
        Key base = new Key(EVEN, SlotAxes.DEFAULT, LAYER_SCALE, DECLARED_SHARE, UNDIVIDED);
        PeriodicLattice baseLattice = noise.latticeIn(table, base);
        List<Key> variants = List.of(
                new Key(ODD, SlotAxes.DEFAULT, LAYER_SCALE, DECLARED_SHARE, UNDIVIDED),
                new Key(EVEN, DensityFunctionSlotAxes.SHIFT_B, LAYER_SCALE, DECLARED_SHARE, UNDIVIDED),
                base.withScale(STARVED_SCALE),
                new Key(EVEN, SlotAxes.DEFAULT, LAYER_SCALE, UNDECLARED, UNDIVIDED),
                new Key(EVEN, SlotAxes.DEFAULT, LAYER_SCALE, DECLARED_SHARE, CELL_DIVISOR));
        for (Key variant : variants) {
            assertNotSame(baseLattice, noise.latticeIn(table, variant), () -> "reused the base lattice for " + variant);
        }
    }

    @Test
    void aTableSampleIsBitIdenticalToAPerCallSample() {
        Random random = new Random(SEED);
        Noise noise = Noise.of(random);
        PeriodicLattices table = new PeriodicLattices();
        for (int i = 0; i < SAMPLES; i++) {
            Key key = KEYS.get(random.nextInt(KEYS.size()));
            double[] yParams = Y_PARAMS[random.nextInt(Y_PARAMS.length)];
            double x = (random.nextDouble() * 2.0 - 1.0) * REACH_BLOCKS;
            double y = random.nextInt(384) - 64 + random.nextDouble();
            double z = (random.nextDouble() * 2.0 - 1.0) * REACH_BLOCKS;
            double cached = key.under(() -> table.sample(noise.permutations(), noise.xo(), noise.yo(), noise.zo(),
                    key.fold(), GenerationTransformerContext.context(), x, y, z, yParams[0], yParams[1]));
            double perCall = key.under(() -> PeriodicNoiseSampler.sample(noise.permutations(), noise.xo(),
                    noise.yo(), noise.zo(), key.fold(), GenerationTransformerContext.context(), x, y, z, yParams[0],
                    yParams[1]));
            assertEquals(perCall, cached, () -> "sample(" + x + ", " + y + ", " + z + ") under " + key);
        }
    }

    @Test
    void aFullTableDropsTheOldestKey() {
        Noise noise = Noise.of(new Random(SEED));
        PeriodicLattices table = new PeriodicLattices();
        PeriodicLattice[] lattices = new PeriodicLattice[PeriodicLattices.CAPACITY + 1];
        for (int i = 0; i < lattices.length; i++) {
            lattices[i] = noise.latticeIn(table, STARVED_TORUS.withScale(STARVED_SCALE * (i + 1)));
        }

        assertSame(lattices[1], noise.latticeIn(table, STARVED_TORUS.withScale(STARVED_SCALE * 2)),
                "dropped a key newer than the oldest");
        assertNotSame(lattices[0], noise.latticeIn(table, STARVED_TORUS.withScale(STARVED_SCALE)),
                "kept the oldest key past the capacity");
    }
}
