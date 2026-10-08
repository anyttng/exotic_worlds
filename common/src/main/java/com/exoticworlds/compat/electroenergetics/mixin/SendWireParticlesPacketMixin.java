package com.exoticworlds.compat.electroenergetics.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.wire.SendWireParticlesPacket;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.client.engine.ClientFrame;
import com.exoticworlds.compat.electroenergetics.WireCopies;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SendWireParticlesPacket.class, remap = false)
public abstract class SendWireParticlesPacketMixin {
    private static final String LIST_GET = "Ljava/util/List;get(I)Ljava/lang/Object;";

    @WrapOperation(method = "handle", at = @At(value = "INVOKE",
            target = "Lcom/george_vi/electroenergetics/foundation/QuadraticWireHelper;cablePoints"
                    + "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Ljava/util/List;"))
    private List<Vec3> toroidal$wireInOnePiece(Vec3 from, Vec3 to, float sag, Operation<List<Vec3>> original,
            @Local(argsOnly = true) LocalPlayer player, @Local(name = "pos2") LocalRef<Vec3> pos2) {
        Vec3 seated = WireCopies.from(WorldLoopAttachments.transformerOfReader(player.level()), from, to,
                WireCopies.FIRST_END).pos2();
        pos2.set(seated);
        return original.call(from, seated, sag);
    }

    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = LIST_GET, ordinal = 0))
    private Object toroidal$sparkNearestThePlayer(List<Vec3> points, int index, Operation<Object> original) {
        return ClientFrame.nearestToPlayer((Vec3) original.call(points, index));
    }

    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = LIST_GET, ordinal = 1))
    private Object toroidal$nextPointBesideIt(List<Vec3> points, int index, Operation<Object> original,
            @Local(name = "point") Vec3 point) {
        return ClientFrame.nearestCopy(point, (Vec3) original.call(points, index));
    }
}
