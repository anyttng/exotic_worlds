package com.exoticworlds.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.accessors.RecipientPositionHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;

@Mixin(GameEventDispatcher.class)
public class GameEventDispatcherMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @WrapOperation(
            method = "handleGameEventMessagesInQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/Collections;sort(Ljava/util/List;)V"))
    private void exotic_worlds$orderThroughSeam(List<GameEvent.ListenerInfo> listenerInfos, Operation<Void> original) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(this.level);
        if (fold != null) {
            listenerInfos.replaceAll(info -> exotic_worlds$nearestCopy(info, fold));
        }

        original.call(listenerInfos);
    }

    @Unique
    private static GameEvent.ListenerInfo exotic_worlds$nearestCopy(GameEvent.ListenerInfo info, WorldFold fold) {
        Vec3 recipientPos = ((RecipientPositionHolder) (Object) info).exotic_worlds$recipientPos();
        Vec3 nearest = fold.nearestCopy(info.source(), recipientPos);
        return nearest == recipientPos
                ? info
                : new GameEvent.ListenerInfo(
                        info.gameEvent(), info.source(), info.context(), info.recipient(), nearest);
    }
}
