package com.exoticworlds.compat.create.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.compat.create.client.CreateClientFrame;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.trains.entity.TrainRelocator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(TrainRelocator.class)
public abstract class TrainRelocatorMixin {
    @ModifyArg(method = "relocate",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
            require = 2,
            allow = 2)
    private static Object exotic_worlds$previewPointInTheViewerFrame(Object point,
            @Local(argsOnly = true) List<Vec3> toVisualise) {
        if (!(point instanceof Vec3 raw)) {
            return point;
        }

        Vec3 anchor = toVisualise.isEmpty() ? exotic_worlds$viewer() : toVisualise.get(toVisualise.size() - 1);
        return anchor == null ? point : CreateClientFrame.nearestCopy(anchor, raw);
    }

    @Unique
    private static @Nullable Vec3 exotic_worlds$viewer() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.position();
    }
}
