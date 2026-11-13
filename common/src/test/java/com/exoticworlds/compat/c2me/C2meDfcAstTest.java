package com.exoticworlds.compat.c2me;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.ishland.c2me.base.common.integration.lithostitched.FNLBindings;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc.GenericFastNoiseNode;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

class C2meDfcAstTest {
    private static final int LAP_BLOCKS = 512;

    private static final FNLBindings.FNLState STATE = new FNLBindings.FNLState(4242, 0.01F, 0, 0, 0, 1, 2.0F, 0.5F,
            0.0F, 2.0F, 1, 1, 1.0F, 0, 1.0F);

    private static final Object CONFIG = new Object();

    @Test
    void anUnfoldedFastNoiseNodeIsReported() {
        assertTrue(C2meDfcAst.reportsMiss(new ForeignNoise(), new GenericFastNoiseNode(CoordinateNode.AXIS_X,
                CoordinateNode.AXIS_Y, CoordinateNode.AXIS_Z, STATE, CONFIG), fold()),
                "a fast noise node C2ME compiled from a foreign function samples the raw coordinate");
    }

    @Test
    void aFoldedFastNoiseNodeIsNotReported() {
        WorldFold fold = fold();
        assertFalse(C2meDfcAst.reportsMiss(new ForeignNoise(), new C2meFoldedFastNoiseNode(CoordinateNode.AXIS_X,
                CoordinateNode.AXIS_Y, CoordinateNode.AXIS_Z, STATE, CONFIG, fold), fold),
                "the folded fast noise node samples the lap");
    }

    private static WorldFold fold() {
        return WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(LAP_BLOCKS)));
    }

    private record ForeignNoise() implements DensityFunction.SimpleFunction {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return 0.0;
        }

        @Override
        public double minValue() {
            return -1.0;
        }

        @Override
        public double maxValue() {
            return 1.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }
}
