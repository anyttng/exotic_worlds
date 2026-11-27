package com.exoticworlds.engine.seam.circumnavigation;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.exoticworlds.accessors.SeamTravelHolder;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.engine.seam.circumnavigation.BoxBands.Band;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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

    public record BandProgress(double aroundA, double aroundB, double aroundC) {
        public static final BandProgress ZERO = new BandProgress(0.0, 0.0, 0.0);

        private static final String AROUND_A_KEY = "around_a";

        private static final String AROUND_B_KEY = "around_b";

        private static final String AROUND_C_KEY = "around_c";

        static final Codec<BandProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf(AROUND_A_KEY).forGetter(BandProgress::aroundA),
                Codec.DOUBLE.fieldOf(AROUND_B_KEY).forGetter(BandProgress::aroundB),
                Codec.DOUBLE.fieldOf(AROUND_C_KEY).forGetter(BandProgress::aroundC))
                .apply(instance, BandProgress::new));

        public double on(Band band) {
            return switch (band) {
                case AROUND_A -> aroundA;
                case AROUND_B -> aroundB;
                case AROUND_C -> aroundC;
            };
        }

        BandProgress with(Band band, double traveled) {
            return switch (band) {
                case AROUND_A -> new BandProgress(traveled, aroundB, aroundC);
                case AROUND_B -> new BandProgress(aroundA, traveled, aroundC);
                case AROUND_C -> new BandProgress(aroundA, aroundB, traveled);
            };
        }
    }

    public record Step(Vec3 folded, Set<Direction.Axis> closed, Set<Band> closedBands) {
        static final Step NONE = new Step(Vec3.ZERO, Set.of(), Set.of());

        public boolean isLapClosed() {
            return !closed.isEmpty() || !closedBands.isEmpty();
        }
    }

    private static final Codec<Either<LapProgress, BandProgress>> PROGRESS_CODEC =
            Codec.either(LapProgress.CODEC, BandProgress.CODEC);

    public static final Codec<SeamTravel> CODEC = Codec.unboundedMap(Level.RESOURCE_KEY_CODEC, PROGRESS_CODEC)
            .xmap(SeamTravel::fromStored, SeamTravel::toStored);

    private final Map<ResourceKey<Level>, LapProgress> progress = new HashMap<>();

    private final Map<ResourceKey<Level>, BandProgress> bandProgress = new HashMap<>();

    private @Nullable BoxBands bands;

    private @Nullable ResourceKey<Level> lastSpace;

    private @Nullable Vec3 lastPosition;

    private static SeamTravel fromStored(Map<ResourceKey<Level>, Either<LapProgress, BandProgress>> stored) {
        SeamTravel travel = new SeamTravel();
        stored.forEach((space, entry) -> entry.ifLeft(laps -> travel.progress.put(space, laps))
                .ifRight(bandLaps -> travel.bandProgress.put(space, bandLaps)));
        return travel;
    }

    private static Map<ResourceKey<Level>, Either<LapProgress, BandProgress>> toStored(SeamTravel travel) {
        Map<ResourceKey<Level>, Either<LapProgress, BandProgress>> stored = new HashMap<>();
        travel.progress.forEach((space, laps) -> stored.put(space, Either.left(laps)));
        travel.bandProgress.forEach((space, bandLaps) -> stored.put(space, Either.right(bandLaps)));
        return stored;
    }

    public static SeamTravel of(ServerPlayer player) {
        return ((SeamTravelHolder) player).exotic_worlds$getTravel();
    }

    public LapProgress getProgressIn(ResourceKey<Level> space) {
        return progress.getOrDefault(space, LapProgress.ZERO);
    }

    public BandProgress getBandsIn(ResourceKey<Level> space) {
        return bandProgress.getOrDefault(space, BandProgress.ZERO);
    }

    public void copyFrom(SeamTravel other) {
        this.progress.clear();
        this.progress.putAll(other.progress);
        this.bandProgress.clear();
        this.bandProgress.putAll(other.bandProgress);
    }

    public Step advance(@Nullable WorldFold fold, ResourceKey<Level> space, Vec3 position) {
        ResourceKey<Level> previousSpace = this.lastSpace;
        Vec3 previousPosition = this.lastPosition;
        this.lastSpace = space;
        this.lastPosition = position;

        if (fold == null || previousPosition == null || !space.equals(previousSpace)) {
            return Step.NONE;
        }

        Vec3 folded = fold.foldDelta(previousPosition, position);
        CuboidAtlas atlas = fold.getCuboidAtlas();
        return atlas == null
                ? travelOnLattice(fold, space, folded)
                : travelOnBox(fold, atlas, space, previousPosition, folded);
    }

    private Step travelOnLattice(WorldFold fold, ResourceKey<Level> space, Vec3 folded) {
        TranslationLattice lattice = fold.getBlockLattice();
        double alongX = lattice.isSkewed()
                ? folded.x - folded.z * lattice.getSkew() / lattice.getZ().domainLength
                : folded.x;
        Set<Direction.Axis> closed = EnumSet.noneOf(Direction.Axis.class);
        LapProgress after = getProgressIn(space);

        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (!fold.getBounds().isLooped(axis)) {
                continue;
            }

            int widthBlocks = lattice.getAxis(axis).domainLength;
            double traveled = after.on(axis) + (axis == Direction.Axis.X ? alongX : folded.z);
            if (Math.abs(traveled) >= widthBlocks) {
                traveled -= Math.signum(traveled) * widthBlocks;
                closed.add(axis);
            }

            after = after.with(axis, traveled);
        }

        progress.put(space, after);
        return new Step(folded, closed, Set.of());
    }

    private Step travelOnBox(WorldFold fold, CuboidAtlas atlas, ResourceKey<Level> space, Vec3 previous,
            Vec3 folded) {
        WorldFold.Folded<Vec3> seat = fold.foldOriented(previous);
        CuboidFace face = atlas.faceAt(Mth.floor(seat.value().x), Mth.floor(seat.value().z));
        if (face == null) {
            return new Step(folded, Set.of(), Set.of());
        }

        Vec3 onFace = seat.orientation().applyToDelta(folded);
        BoxBands box = bandsOf(atlas);
        Set<Band> closed = EnumSet.noneOf(Band.class);
        BandProgress after = getBandsIn(space);

        for (Band band : Band.values()) {
            double traveled = BoxBands.isCap(face, band)
                    ? 0.0
                    : after.on(band) + BoxBands.progressAround(face, band, onFace.x, onFace.z);
            int lapBlocks = box.getLapBlocks(band);
            if (Math.abs(traveled) >= lapBlocks) {
                traveled -= Math.signum(traveled) * lapBlocks;
                closed.add(band);
            }

            after = after.with(band, traveled);
        }

        bandProgress.put(space, after);
        return new Step(folded, Set.of(), closed);
    }

    private BoxBands bandsOf(CuboidAtlas atlas) {
        BoxBands known = this.bands;
        if (known != null && known.isOf(atlas)) {
            return known;
        }

        BoxBands built = BoxBands.of(atlas);
        this.bands = built;
        return built;
    }
}
