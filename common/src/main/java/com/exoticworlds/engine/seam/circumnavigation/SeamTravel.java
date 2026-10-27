package com.exoticworlds.engine.seam.circumnavigation;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.exoticworlds.accessors.SeamTravelHolder;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SeamTravel {
    public record LapProgress(double x, double z) {
        public static final LapProgress ZERO = new LapProgress(0.0, 0.0);

        private static final String X_KEY = "x";

        private static final String Z_KEY = "z";

        static final Codec<LapProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf(X_KEY).forGetter(LapProgress::x),
                Codec.DOUBLE.fieldOf(Z_KEY).forGetter(LapProgress::z))
                .apply(instance, LapProgress::new));

        public double on(Direction.Axis axis) {
            return axis == Direction.Axis.X ? x : z;
        }

        LapProgress with(Direction.Axis axis, double traveled) {
            return axis == Direction.Axis.X ? new LapProgress(traveled, z) : new LapProgress(x, traveled);
        }
    }

    public record Step(Vec3 folded, Set<Direction.Axis> closed) {
    }

    public static final Codec<SeamTravel> CODEC = Codec.unboundedMap(Level.RESOURCE_KEY_CODEC, LapProgress.CODEC)
            .xmap(SeamTravel::new, travel -> travel.progress);

    private final Map<ResourceKey<Level>, LapProgress> progress;

    private @Nullable ResourceKey<Level> lastSpace;

    private @Nullable Vec3 lastPosition;

    public SeamTravel() {
        this(Map.of());
    }

    private SeamTravel(Map<ResourceKey<Level>, LapProgress> progress) {
        this.progress = new HashMap<>(progress);
    }

    public static SeamTravel of(ServerPlayer player) {
        return ((SeamTravelHolder) player).exotic_worlds$travel();
    }

    public LapProgress in(ResourceKey<Level> space) {
        return progress.getOrDefault(space, LapProgress.ZERO);
    }

    public void copyFrom(SeamTravel other) {
        this.progress.clear();
        this.progress.putAll(other.progress);
    }

    public Step advance(@Nullable WorldFold fold, ResourceKey<Level> space, Vec3 position) {
        ResourceKey<Level> previousSpace = this.lastSpace;
        Vec3 previousPosition = this.lastPosition;
        this.lastSpace = space;
        this.lastPosition = position;

        if (fold == null || previousPosition == null || !space.equals(previousSpace)) {
            return new Step(Vec3.ZERO, Set.of());
        }

        return travel(fold, space, fold.foldDelta(previousPosition, position));
    }

    private Step travel(WorldFold fold, ResourceKey<Level> space, Vec3 folded) {
        TranslationLattice lattice = fold.blockLattice();
        double alongX = lattice.isSkewed()
                ? folded.x - folded.z * lattice.skew() / lattice.z().domainLength
                : folded.x;
        Set<Direction.Axis> closed = EnumSet.noneOf(Direction.Axis.class);
        LapProgress after = in(space);

        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (!fold.bounds().loops(axis)) {
                continue;
            }

            int width = axis == Direction.Axis.X ? lattice.x().domainLength : lattice.z().domainLength;
            double traveled = after.on(axis) + (axis == Direction.Axis.X ? alongX : folded.z);
            if (Math.abs(traveled) >= width) {
                traveled -= Math.signum(traveled) * width;
                closed.add(axis);
            }

            after = after.with(axis, traveled);
        }

        progress.put(space, after);
        return new Step(folded, closed);
    }
}
