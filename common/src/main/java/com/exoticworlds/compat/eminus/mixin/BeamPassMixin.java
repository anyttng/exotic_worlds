package com.exoticworlds.compat.eminus.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.client.frame.GameFrame;
import com.eminus.client.session.ClientSession;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.EminusBeams;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.ClientLevel;

@Pseudo
@Mixin(targets = "com.eminus.client.render.beam.BeamPass", remap = false)
public class BeamPassMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lcom/eminus/client/render/beam/BeamPass;unseenByTheGame(Ljava/util/List;"
                    + "Lnet/minecraft/client/multiplayer/ClientLevel;Lcom/eminus/client/frame/GameFrame;)"
                    + "Ljava/util/List;"))
    private List<Object> exotic_worlds$drawEachBeamAtItsLapCopy(List<Object> beams, ClientLevel level,
            GameFrame game, Operation<List<Object>> original) {
        CellFold fold = CellFoldHolder.foldOf(ClientSession.runtime());
        List<Object> copies = new ArrayList<>(beams.size());
        for (Object beam : beams) {
            copies.add(EminusBeams.at(beam, fold.copyNear(EminusBeams.pos(beam), game.eyeX(), game.eyeZ())));
        }

        return original.call(copies, level, game);
    }
}
