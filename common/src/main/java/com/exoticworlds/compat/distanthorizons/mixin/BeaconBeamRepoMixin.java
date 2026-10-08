package com.exoticworlds.compat.distanthorizons.mixin;

import java.sql.PreparedStatement;
import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.distanthorizons.DhKeys;
import com.exoticworlds.compat.distanthorizons.DhRepoLevel;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import com.seibel.distanthorizons.core.sql.repo.BeaconBeamRepo;

@Mixin(BeaconBeamRepo.class)
public class BeaconBeamRepoMixin {
    @WrapMethod(method = "setPreparedStatementWhereClause(Ljava/sql/PreparedStatement;ILcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos;)I")
    private int toroidal$foldWhereKey(PreparedStatement statement, int index, DhBlockPos pos,
            Operation<Integer> original) {
        return original.call(statement, index, DhKeys.foldBlock(DhRepoLevel.shapeOf(this), pos));
    }

    @WrapMethod(method = "createUpsertStatement(Lcom/seibel/distanthorizons/core/sql/dto/BeaconBeamDTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement toroidal$foldUpsert(BeaconBeamDTO dto, Operation<PreparedStatement> original) {
        return DhKeys.withFoldedKey(DhRepoLevel.shapeOf(this), dto, () -> original.call(dto));
    }

    @WrapMethod(method = "getAllBeamsInBlockPosRange")
    private ArrayList<BeaconBeamDTO> toroidal$foldRange(int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ,
            Operation<ArrayList<BeaconBeamDTO>> original) {
        ToroidalShape shape = DhRepoLevel.shapeOf(this);
        if (shape == null) {
            return original.call(minBlockX, maxBlockX, minBlockZ, maxBlockZ);
        }

        ArrayList<BeaconBeamDTO> beams = new ArrayList<>();
        for (WorldCopies.Piece piece : WorldCopies.pieces(shape, minBlockX, minBlockZ, maxBlockX, maxBlockZ)) {
            WorldCopies.Copy copy = piece.copy();
            for (BeaconBeamDTO beam : original.call(piece.minX(), piece.maxX(), piece.minZ(), piece.maxZ())) {
                if (!copy.isIdentity()) {
                    beam.blockPos = new DhBlockPos(beam.blockPos.getX() + copy.dx(), beam.blockPos.getY(),
                            beam.blockPos.getZ() + copy.dz());
                }

                beams.add(beam);
            }
        }

        return beams;
    }
}
