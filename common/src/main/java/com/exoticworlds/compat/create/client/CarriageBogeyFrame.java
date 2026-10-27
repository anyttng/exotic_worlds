package com.exoticworlds.compat.create.client;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.compat.trainmap.TrainMapSync.TrainMapSyncEntry;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.phys.Vec3;

public final class CarriageBogeyFrame {
    private @Nullable Object train;

    private int carriageIndex;

    private @Nullable Vec3 leading;

    public static Vec3 inOneFrame(TrainMapSyncEntry entry, int carriageIndex, boolean firstBogey,
            double time, Operation<Vec3> original, LocalRef<CarriageBogeyFrame> frameRef) {
        CarriageBogeyFrame frame = frameRef.get();
        if (frame == null) {
            frame = new CarriageBogeyFrame();
            frameRef.set(frame);
        }

        WorldFold fold = TrainMapViewFold.fold();
        Vec3 raw = original.call(entry, carriageIndex, firstBogey, time);
        return firstBogey
                ? frame.lead(fold, entry, carriageIndex, raw)
                : frame.trail(fold, entry, carriageIndex, raw,
                        () -> original.call(entry, carriageIndex, true, time));
    }

    public Vec3 lead(WorldFold fold, Object leadingTrain, int leadingCarriage, Vec3 raw) {
        Vec3 canonical = fold.fold(raw);
        this.train = leadingTrain;
        this.carriageIndex = leadingCarriage;
        this.leading = canonical;
        return canonical;
    }

    public Vec3 trail(WorldFold fold, Object trailingTrain, int trailingCarriage, Vec3 raw,
            Supplier<Vec3> leadingBogey) {
        Vec3 known = this.leading;
        if (known == null || this.train != trailingTrain || this.carriageIndex != trailingCarriage) {
            known = fold.fold(leadingBogey.get());
        }

        return fold.nearestCopy(known, raw);
    }
}
