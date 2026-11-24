package com.exoticworlds.compat.distanthorizons.mixin;

import java.sql.PreparedStatement;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.distanthorizons.DhKeys;
import com.exoticworlds.compat.distanthorizons.DhRepoLevel;
import com.exoticworlds.compat.distanthorizons.DhSeamSql;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

@Mixin(FullDataSourceV2Repo.class)
public class FullDataSourceV2RepoMixin {
    @Unique
    private static final String CREATE_PREPARED_STATEMENT = "Lcom/seibel/distanthorizons/core/sql/repo/FullDataSourceV2Repo;"
            + "createPreparedStatement(Ljava/lang/String;)Ljava/sql/PreparedStatement;";

    @WrapMethod(method = "setPreparedStatementWhereClause(Ljava/sql/PreparedStatement;ILjava/lang/Long;)I")
    private int exotic_worlds$foldWhereKey(PreparedStatement statement, int index, Long pos, Operation<Integer> original) {
        return original.call(statement, index, DhKeys.foldSection(DhRepoLevel.getLattice(this), pos));
    }

    @WrapMethod(method = "createUpsertStatement(Lcom/seibel/distanthorizons/core/sql/dto/FullDataSourceV2DTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement exotic_worlds$foldUpsert(FullDataSourceV2DTO dto, Operation<PreparedStatement> original) {
        Object raw = DhKeys.seatFolded(DhRepoLevel.getLattice(this), dto);
        try {
            return original.call(dto);
        } finally {
            DhKeys.reseat(dto, raw);
        }
    }

    @WrapMethod(method = "getAdjByPosAndDirection")
    private @Nullable FullDataSourceV2DTO exotic_worlds$foldAdjacent(long pos, EDhDirection direction,
            Operation<@Nullable FullDataSourceV2DTO> original) {
        FullDataSourceV2DTO dto = original.call(DhKeys.foldSection(DhRepoLevel.getLattice(this), pos), direction);
        if (dto != null) {
            dto.pos = pos;
        }

        return dto;
    }

    @WrapMethod(method = "setApplyToParent")
    private void exotic_worlds$foldApplyToParent(long pos, boolean shouldApplyToParent, Operation<Void> original) {
        original.call(DhKeys.foldSection(DhRepoLevel.getLattice(this), pos), shouldApplyToParent);
    }

    @WrapMethod(method = "setApplyToChild")
    private void exotic_worlds$foldApplyToChild(long pos, boolean shouldApplyToChild, Operation<Void> original) {
        original.call(DhKeys.foldSection(DhRepoLevel.getLattice(this), pos), shouldApplyToChild);
    }

    @WrapMethod(method = "setRegenerate")
    private void exotic_worlds$foldRegenerate(long pos, boolean shouldRegenerate, Operation<Void> original) {
        original.call(DhKeys.foldSection(DhRepoLevel.getLattice(this), pos), shouldRegenerate);
    }

    @WrapMethod(method = "getColumnGenerationStepForPos")
    private void exotic_worlds$foldGenerationStepPos(long pos, ByteArrayList output, Operation<Void> original) {
        original.call(DhKeys.foldSection(DhRepoLevel.getLattice(this), pos), output);
    }

    @WrapMethod(method = "getTimestampForPos")
    private Long exotic_worlds$foldTimestampPos(long pos, Operation<Long> original) {
        return original.call(DhKeys.foldSection(DhRepoLevel.getLattice(this), pos));
    }

    @WrapMethod(method = "getDataSizeInBytes")
    private long exotic_worlds$foldDataSizePos(long pos, Operation<Long> original) {
        return original.call(DhKeys.foldSection(DhRepoLevel.getLattice(this), pos));
    }

    @WrapMethod(method = "getPositionsToUpdate(IIILjava/lang/String;)Lit/unimi/dsi/fastutil/longs/LongArrayList;")
    private LongArrayList exotic_worlds$rankUpdatesThroughTheSeam(int targetBlockX, int targetBlockZ, int returnCount,
            String sql, Operation<LongArrayList> original) {
        return original.call(targetBlockX, targetBlockZ, returnCount,
                DhSeamSql.rewrite(DhRepoLevel.getLattice(this), sql, DhSeamSql.Site.UPDATE));
    }

    @WrapOperation(method = "getChildPositionsToRegen", at = @At(value = "INVOKE", target = CREATE_PREPARED_STATEMENT))
    private PreparedStatement exotic_worlds$filterRegenThroughTheSeam(FullDataSourceV2Repo repo, String sql,
            Operation<PreparedStatement> original) {
        return original.call(repo, DhSeamSql.rewrite(DhRepoLevel.getLattice(this), sql, DhSeamSql.Site.REGEN));
    }

    @WrapOperation(method = "getRegenChunkCount", at = @At(value = "INVOKE", target = CREATE_PREPARED_STATEMENT))
    private PreparedStatement exotic_worlds$countRegenThroughTheSeam(FullDataSourceV2Repo repo, String sql,
            Operation<PreparedStatement> original) {
        return original.call(repo, DhSeamSql.rewrite(DhRepoLevel.getLattice(this), sql, DhSeamSql.Site.REGEN_COUNT));
    }
}
