package com.exoticworlds.compat.eminus.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.beam.Beam;
import com.eminus.client.frame.GameFrame;
import com.eminus.client.render.beam.BeamPass;
import com.eminus.client.session.ClientSession;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.ClientLevel;

@Mixin(BeamPass.class)
public class BeamPassMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lcom/eminus/client/render/beam/BeamPass;unseenByTheGame(Ljava/util/List;"
                    + "Lnet/minecraft/client/multiplayer/ClientLevel;Lcom/eminus/client/frame/GameFrame;)"
                    + "Ljava/util/List;"))
    private List<Beam> exotic_worlds$drawEachBeamAtItsLapCopy(List<Beam> beams, ClientLevel level,
            GameFrame game, Operation<List<Beam>> original) {
        CellFold fold = CellFoldHolder.getFold(ClientSession.runtime());
        List<Beam> copies = new ArrayList<>(beams.size());
        for (Beam beam : beams) {
            copies.add(new Beam(fold.copyNear(beam.pos(), game.eyeX(), game.eyeZ()), beam.sections()));
        }

        return original.call(copies, level, game);
    }
}
