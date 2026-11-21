package com.exoticworlds.engine.noise;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.DoubleStack;
import com.exoticworlds.engine.ObjectStack;

public final class GenerationFoldContext {
    public static final double UNDECLARED_VERTICAL_SHARE = -1.0;

    public static double verticalShare(double xzScale, double yScale) {
        return xzScale == 0.0 ? UNDECLARED_VERTICAL_SHARE : yScale / xzScale;
    }

    public static final class Context {
        private WorldFold fold = WorldFolds.NOOP;
        private @Nullable WorldFold routerBuild;
        private double horizontalScale = NoiseConstants.UNSCALED;
        private double verticalShare = UNDECLARED_VERTICAL_SHARE;
        private double xDivisor = NoiseConstants.UNDIVIDED;
        private double zDivisor = NoiseConstants.UNDIVIDED;
        private BoxDivisors boxDivisors = BoxDivisors.UNDIVIDED;
        private SlotAxes slotAxes = SlotAxes.DEFAULT;
        private final ScaleScope scaleScope = new ScaleScope();
        private final DivisorScope divisorScope = new DivisorScope();
        private final BindingScope bindingScope = new BindingScope();
        private final FoldScope foldScope = new FoldScope();

        public WorldFold fold() {
            return this.fold;
        }

        public @Nullable WorldFold wrappedFold() {
            return this.fold.isWrapped() ? this.fold : null;
        }

        public @Nullable WorldFold routerBuildFold() {
            return this.routerBuild;
        }

        // mc/26.2: the noise samplers and C2ME's compiled tree read the scale through it.
        public double horizontalScale() {
            return this.horizontalScale;
        }

        public double verticalShare() {
            return this.verticalShare;
        }

        public double xDivisor() {
            return this.xDivisor;
        }

        public double zDivisor() {
            return this.zDivisor;
        }

        public BoxDivisors boxDivisors() {
            return this.boxDivisors;
        }

        // mc/26.2: the noise samplers read the slot axes through it.
        public SlotAxes slotAxes() {
            return this.slotAxes;
        }

        public FoldScope withFold(WorldFold boundFold) {
            this.foldScope.push();
            this.fold = boundFold;
            return this.foldScope;
        }

        public ScaleScope withScale(double scale) {
            this.scaleScope.push();
            this.horizontalScale = scale;
            return this.scaleScope;
        }

        public ScaleScope withScale(double scale, double verticalShare) {
            this.scaleScope.push();
            this.horizontalScale = scale;
            this.verticalShare = verticalShare;
            return this.scaleScope;
        }

        public DivisorScope withDivisors(double xDivisor, double zDivisor) {
            this.divisorScope.push();
            this.xDivisor = xDivisor;
            this.zDivisor = zDivisor;
            return this.divisorScope;
        }

        public DivisorScope withBoxDivisors(BoxDivisors boxDivisors) {
            this.divisorScope.push();
            this.boxDivisors = boxDivisors;
            return this.divisorScope;
        }

        // mc/26.2: the octave samplers open their per-octave scale scope through it.
        public ScaleScope openScale() {
            this.scaleScope.push();
            return this.scaleScope;
        }

        public BindingScope bind(WorldFold boundFold, SlotAxes boundAxes, double boundScale,
                double boundVerticalShare) {
            this.bindingScope.push();
            this.fold = boundFold;
            this.slotAxes = boundAxes;
            this.horizontalScale = boundScale;
            this.verticalShare = boundVerticalShare;
            return this.bindingScope;
        }

        public final class BindingScope implements AutoCloseable {
            private final ObjectStack<WorldFold> previousFolds = new ObjectStack<>();
            private final ObjectStack<SlotAxes> previousAxes = new ObjectStack<>();
            private final DoubleStack previousScales = new DoubleStack();
            private final DoubleStack previousShares = new DoubleStack();

            private BindingScope() {
            }

            private void push() {
                this.previousFolds.push(fold);
                this.previousAxes.push(slotAxes);
                this.previousScales.push(horizontalScale);
                this.previousShares.push(verticalShare);
            }

            @Override
            public void close() {
                fold = this.previousFolds.pop();
                slotAxes = this.previousAxes.pop();
                horizontalScale = this.previousScales.pop();
                verticalShare = this.previousShares.pop();
            }
        }

        public final class FoldScope implements AutoCloseable {
            private final ObjectStack<WorldFold> previousFolds = new ObjectStack<>();

            private FoldScope() {
            }

            private void push() {
                this.previousFolds.push(fold);
            }

            @Override
            public void close() {
                fold = this.previousFolds.pop();
            }
        }

        public final class ScaleScope implements AutoCloseable {
            private final DoubleStack previousScales = new DoubleStack();
            private final DoubleStack previousShares = new DoubleStack();

            private ScaleScope() {
            }

            private void push() {
                this.previousScales.push(horizontalScale);
                this.previousShares.push(verticalShare);
            }

            // mc/26.2: the octave samplers set each octave's scale through it.
            public void rescale(double scale) {
                if (this.previousScales.isEmpty()) {
                    throw new IllegalStateException("rescale with no scale scope open");
                }

                horizontalScale = scale;
            }

            @Override
            public void close() {
                horizontalScale = this.previousScales.pop();
                verticalShare = this.previousShares.pop();
            }
        }

        public final class DivisorScope implements AutoCloseable {
            private final DoubleStack previousXDivisors = new DoubleStack();
            private final DoubleStack previousZDivisors = new DoubleStack();
            private final ObjectStack<BoxDivisors> previousBoxDivisors = new ObjectStack<>();

            private DivisorScope() {
            }

            private void push() {
                this.previousXDivisors.push(xDivisor);
                this.previousZDivisors.push(zDivisor);
                this.previousBoxDivisors.push(boxDivisors);
            }

            @Override
            public void close() {
                xDivisor = this.previousXDivisors.pop();
                zDivisor = this.previousZDivisors.pop();
                boxDivisors = this.previousBoxDivisors.pop();
            }
        }
    }

    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);

    public static Context context() {
        return CONTEXT.get();
    }

    public static @Nullable WorldFold carriedOrBound(@Nullable WorldFold carried) {
        return carried != null ? carried : CONTEXT.get().wrappedFold();
    }

    public static <T> T withFold(WorldFold fold, Supplier<T> action) {
        try (Context.FoldScope _ = CONTEXT.get().withFold(fold)) {
            return action.get();
        }
    }

    public static void runWithFold(WorldFold fold, Runnable action) {
        try (Context.FoldScope _ = CONTEXT.get().withFold(fold)) {
            action.run();
        }
    }

    public static <T> T withRouterBuild(@Nullable WorldFold fold, Supplier<T> action) {
        Context context = CONTEXT.get();
        WorldFold previous = context.routerBuild;
        context.routerBuild = fold;

        try {
            return action.get();
        } finally {
            context.routerBuild = previous;
        }
    }

    private GenerationFoldContext() {
    }
}
