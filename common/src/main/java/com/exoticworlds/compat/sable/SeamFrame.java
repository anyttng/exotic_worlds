package com.exoticworlds.compat.sable;

import java.util.function.Function;
import java.util.function.Supplier;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.ForeignFrames;
import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.ThreadScope;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public final class SeamFrame {
    public static final Object KEPT = new Object();

    private static final ThreadScope<Binding<?>> BOUND = new ThreadScope<>();

    private static boolean everBound;

    private static final class Binding<A> {
        private final WorldFold fold;
        private final @Nullable Level level;
        private final A subject;
        private final Function<? super A, Vec3> source;

        private @Nullable Vec3 seated;
        private boolean seating;
        private @Nullable PoseSeat lastPose;

        private Binding(WorldFold fold, @Nullable Level level, A subject, Function<? super A, Vec3> source) {
            this.fold = fold;
            this.level = level;
            this.subject = subject;
            this.source = source;
        }

        private Vec3 anchor() {
            Vec3 known = this.seated;
            if (known != null) {
                return known;
            }

            Vec3 raw = this.source.apply(this.subject);
            if (this.level == null) {
                this.seated = raw;
                return raw;
            }

            this.seating = true;
            try {
                known = ForeignFrames.seatInWorld(this.level, raw);
            } finally {
                this.seating = false;
            }

            this.seated = known;
            return known;
        }

        private Vector3dc seatPose(Vector3dc position) {
            PoseSeat last = this.lastPose;
            if (last == null || !last.holds(position)) {
                Vec3 raw = JomlVectors.read(position);
                DeckTransformation seat = this.fold.nearestCopyTransformation(anchor(), raw);
                last = new PoseSeat(raw.x, raw.y, raw.z,
                        seat.isIdentity() ? null : JomlVectors.write(seat.apply(raw), new Vector3d()));
                this.lastPose = last;
            }

            Vector3dc shifted = last.seated();
            return shifted == null ? position : shifted;
        }
    }

    private record PoseSeat(double x, double y, double z, @Nullable Vector3dc seated) {
        private boolean holds(Vector3dc position) {
            return position.x() == this.x && position.y() == this.y && position.z() == this.z;
        }
    }

    public static <A> @Nullable Object enter(LevelReader reader, A subject, Function<? super A, Vec3> anchor) {
        return enter(serverFoldOf(reader), reader instanceof Level level ? level : null, subject, anchor);
    }

    public static <A> @Nullable Object enter(@Nullable WorldFold fold, @Nullable Level level, A subject,
            Function<? super A, Vec3> anchor) {
        if (fold != null) {
            everBound = true;
            return BOUND.enter(new Binding<>(fold, level, subject, anchor));
        }

        return isBound() ? BOUND.enter(null) : KEPT;
    }

    public static void exit(@Nullable Object entered) {
        if (entered != KEPT) {
            BOUND.exit((Binding<?>) entered);
        }
    }

    public static <R> R unbound(Supplier<R> body) {
        return isBound() ? BOUND.with(null, body) : body.get();
    }

    public static boolean isBound() {
        return everBound && BOUND.current() != null;
    }

    public static Vector3dc seatPose(Vector3dc position) {
        if (!everBound) {
            return position;
        }

        Binding<?> binding = BOUND.current();
        if (binding == null || binding.seating) {
            return position;
        }

        return binding.seatPose(position);
    }

    private static @Nullable WorldFold serverFoldOf(LevelReader reader) {
        return WorldLoopAttachments.wrappedFoldOf(WorldLoopAttachments.serverLevelOf(reader));
    }

    private SeamFrame() {
    }
}
