package com.exoticworlds.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo;

import it.unimi.dsi.fastutil.longs.LongArrayList;

class DhSeamSqlTest {
    private static final int WIDTH_BLOCKS = 1024;
    private static final int HALF_CHUNKS = WIDTH_BLOCKS / 16 / 2;
    private static final int FIRST_LEAF = -8;
    private static final int LAST_LEAF = 7;
    private static final int NEAR_THE_SEAM = 500;
    private static final int REGEN_REACH = 80;
    private static final int CHUNKS_PER_LEAF_ROW = 64;
    private static final int LEAF_SKEW_CHUNKS = 4;
    private static final int LEAF_SKEW_BLOCKS = LEAF_SKEW_CHUNKS * 16;
    private static final int LEAF_BLOCKS = 64;
    private static final int LAPS = 3;

    private static final String DATABASE_TYPE = "jdbc:dh_sqlite";
    private static final String DATABASE_FILE = "dh.sqlite";
    private static final String INSERT_LEAF = "INSERT INTO FullData (DetailLevel, PosX, PosZ, MinY, DataChecksum,"
            + " LastModifiedUnixDateTime, CreatedUnixDateTime, ApplyToParent, ApplyToChildren, Regenerate)"
            + " VALUES (0, ?, ?, 0, 0, 0, 0, 1, 1, 1)";
    private static final String PARENT_SQL = "getParentPositionsToUpdateSql";
    private static final String REGEN_SQL = "getRegenPositionsToUpdateSql";
    private static final String REGEN_COUNT_SQL = "getRegenChunkCountLimitedSql";

    @TempDir
    Path dir;

    private static DhLattice torus() {
        AxisBounds.Looped looped = new AxisBounds.Looped(-HALF_CHUNKS, HALF_CHUNKS);
        return DhLattice.of(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(looped, looped))));
    }

    private static DhLattice cylinder() {
        AxisBounds.Looped looped = new AxisBounds.Looped(-HALF_CHUNKS, HALF_CHUNKS);
        return DhLattice.of(WorldFolds.of(
                FlatShape.torus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE))));
    }

    private static DhLattice lattice() {
        AxisBounds.Looped looped = new AxisBounds.Looped(-HALF_CHUNKS, HALF_CHUNKS);
        return DhLattice.of(WorldFolds.of(
                FlatShape.latticeTorus(new WorldLoopBounds(looped, looped), LEAF_SKEW_CHUNKS)));
    }

    @Test
    void aSkewedWorldTakesTheRowOneZLapOnWhereTheSkewMovesIt() throws Exception {
        try (FullDataSourceV2Repo repo = repoWithLeafRowsAlongX(lattice(), PARENT_SQL, DhSeamSql.Site.UPDATE)) {
            assertEquals(List.of(-1), xs(repo.getParentPositionsToUpdate(0, WIDTH_BLOCKS - 24, 1)));
        }
    }

    @Test
    void aSkewedWorldRanksByTheShortestDistanceOverEveryLatticeCopy() throws Exception {
        try (FullDataSourceV2Repo repo = repoWithLeafRowsAlongX(lattice(), PARENT_SQL, DhSeamSql.Site.UPDATE)) {
            for (int targetX = -700; targetX <= 700; targetX += 233) {
                for (int targetZ = -1500; targetZ <= 1500; targetZ += 271) {
                    LongArrayList ranked =
                            repo.getParentPositionsToUpdate(targetX, targetZ, LAST_LEAF - FIRST_LEAF + 1);
                    long previous = Long.MIN_VALUE;
                    for (int x : xs(ranked)) {
                        long distance = latticeManhattan(x * LEAF_BLOCKS - targetX, -targetZ);
                        assertTrue(previous <= distance, "target " + targetX + "," + targetZ + " row " + x);
                        previous = distance;
                    }
                }
            }
        }
    }

    private static long latticeManhattan(long deltaX, long deltaZ) {
        long nearest = Long.MAX_VALUE;
        for (int i = -LAPS; i <= LAPS; i++) {
            for (int j = -LAPS; j <= LAPS; j++) {
                long copyX = deltaX + (long) i * WIDTH_BLOCKS + (long) j * LEAF_SKEW_BLOCKS;
                long copyZ = deltaZ + (long) j * WIDTH_BLOCKS;
                nearest = Math.min(nearest, Math.abs(copyX) + Math.abs(copyZ));
            }
        }

        return nearest;
    }

    @Test
    void propagationTakesTheRowsJustPastTheSeamFirst() throws Exception {
        try (FullDataSourceV2Repo repo = repoWithLeafRowsAlongX(torus(), PARENT_SQL, DhSeamSql.Site.UPDATE)) {
            assertEquals(List.of(-8, 7, -7), xs(repo.getParentPositionsToUpdate(NEAR_THE_SEAM, 0, 3)));
        }
    }

    @Test
    void propagationAwayFromTheSeamKeepsDhsOwnOrder() throws Exception {
        try (FullDataSourceV2Repo repo = repoWithLeafRowsAlongX(torus(), PARENT_SQL, DhSeamSql.Site.UPDATE)) {
            assertEquals(List.of(0, 1, -1), xs(repo.getParentPositionsToUpdate(10, 0, 3)));
        }
    }

    @Test
    void regenerationOneLapOnFindsTheRowsAroundThePlayer() throws Exception {
        try (FullDataSourceV2Repo repo = repoWithLeafRowsAlongX(torus(), REGEN_SQL, DhSeamSql.Site.REGEN)) {
            assertEquals(List.of(-8, 7, -7),
                    xs(repo.getChildPositionsToRegen(NEAR_THE_SEAM + WIDTH_BLOCKS, 0, REGEN_REACH, 10)));
        }
    }

    @Test
    void regenerationMeasuresTheZAxisThroughItsSeamToo() throws Exception {
        try (FullDataSourceV2Repo repo = repoWithLeafRowsAlongZ(torus(), REGEN_SQL, DhSeamSql.Site.REGEN)) {
            assertEquals(List.of(-8, 7, -7),
                    zs(repo.getChildPositionsToRegen(0, NEAR_THE_SEAM - 2 * WIDTH_BLOCKS, REGEN_REACH, 10)));
        }
    }

    @Test
    void theRegenCountHoldsEachRowWithinReachOnce() throws Exception {
        try (FullDataSourceV2Repo repo = repoWithLeafRowsAlongX(torus(), REGEN_COUNT_SQL, DhSeamSql.Site.REGEN_COUNT)) {
            assertEquals(3L * CHUNKS_PER_LEAF_ROW,
                    repo.getRegenChunkCount(NEAR_THE_SEAM + WIDTH_BLOCKS, 0, REGEN_REACH));
        }
    }

    @Test
    void theLoopingAxisOfACylinderMeasuresThroughTheSeam() throws Exception {
        try (FullDataSourceV2Repo repo = repoWithLeafRowsAlongX(cylinder(), REGEN_SQL, DhSeamSql.Site.REGEN)) {
            assertEquals(List.of(-8, 7, -7),
                    xs(repo.getChildPositionsToRegen(NEAR_THE_SEAM + WIDTH_BLOCKS, 0, REGEN_REACH, 10)));
        }
    }

    @Test
    void theUnboundedAxisOfACylinderKeepsThePlainDistance() throws Exception {
        try (FullDataSourceV2Repo repo = repoWithLeafRowsAlongZ(cylinder(), REGEN_SQL, DhSeamSql.Site.REGEN)) {
            assertEquals(List.of(),
                    zs(repo.getChildPositionsToRegen(0, NEAR_THE_SEAM + WIDTH_BLOCKS, REGEN_REACH, 10)));
            assertEquals(List.of(7),
                    zs(repo.getChildPositionsToRegen(0, NEAR_THE_SEAM, REGEN_REACH, 10)));
        }
    }

    @Test
    void aStatementWithoutDhsDistanceComesBackUntouched() {
        String sql = "SELECT 1";
        assertSame(sql, DhSeamSql.rewrite(torus(), sql, DhSeamSql.Site.UPDATE));
    }

    @Test
    void anUnwrappedLevelKeepsDhsStatement() throws Exception {
        try (FullDataSourceV2Repo repo = openRepo()) {
            String sql = sqlOf(repo, REGEN_SQL);
            assertSame(sql, DhSeamSql.rewrite(null, sql, DhSeamSql.Site.REGEN));
        }
    }

    private FullDataSourceV2Repo repoWithLeafRowsAlongX(DhLattice lattice, String sqlField, DhSeamSql.Site site)
            throws Exception {
        FullDataSourceV2Repo repo = openRepo();
        for (int x = FIRST_LEAF; x <= LAST_LEAF; x++) {
            insertLeaf(repo, x, 0);
        }

        rewriteInPlace(repo, lattice, sqlField, site);
        return repo;
    }

    private FullDataSourceV2Repo repoWithLeafRowsAlongZ(DhLattice lattice, String sqlField, DhSeamSql.Site site)
            throws Exception {
        FullDataSourceV2Repo repo = openRepo();
        for (int z = FIRST_LEAF; z <= LAST_LEAF; z++) {
            insertLeaf(repo, 0, z);
        }

        rewriteInPlace(repo, lattice, sqlField, site);
        return repo;
    }

    private FullDataSourceV2Repo openRepo() throws Exception {
        return new FullDataSourceV2Repo(DATABASE_TYPE, this.dir.resolve(DATABASE_FILE).toFile());
    }

    private static void insertLeaf(FullDataSourceV2Repo repo, int x, int z) throws Exception {
        try (PreparedStatement statement = repo.getConnection().prepareStatement(INSERT_LEAF)) {
            statement.setInt(1, x);
            statement.setInt(2, z);
            statement.executeUpdate();
        }
    }

    private static void rewriteInPlace(FullDataSourceV2Repo repo, DhLattice lattice, String sqlField,
            DhSeamSql.Site site) throws Exception {
        Field field = FullDataSourceV2Repo.class.getDeclaredField(sqlField);
        field.setAccessible(true);
        field.set(repo, DhSeamSql.rewrite(lattice, (String) field.get(repo), site));
    }

    private static String sqlOf(FullDataSourceV2Repo repo, String sqlField) throws Exception {
        Field field = FullDataSourceV2Repo.class.getDeclaredField(sqlField);
        field.setAccessible(true);
        return (String) field.get(repo);
    }

    private static List<Integer> xs(LongArrayList positions) {
        List<Integer> xs = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            xs.add(DhSectionPos.getX(positions.getLong(i)));
        }

        return xs;
    }

    private static List<Integer> zs(LongArrayList positions) {
        List<Integer> zs = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            zs.add(DhSectionPos.getZ(positions.getLong(i)));
        }

        return zs;
    }
}
