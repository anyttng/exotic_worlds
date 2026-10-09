package com.exoticworlds.compat.distanthorizons;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.api.v1.ToroidalShape;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;

import net.minecraft.core.Direction;

public final class DhProbes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NONE = "none";

    private static final Set<String> SEEN_KEY_PERIODS = ConcurrentHashMap.newKeySet();
    private static final Set<DhSeamSql.Site> SEEN_SEAM_SQL_SITES = ConcurrentHashMap.newKeySet();

    enum Key {
        SECTION("section"),
        CHUNK("chunk"),
        BEACON("beacon");

        private final String label;
        private final AtomicInteger lines = new AtomicInteger();
        private final LongAdder kept = new LongAdder();

        Key(String label) {
            this.label = label;
        }

        private boolean firstFold() {
            return this.lines.get() == 0 && this.lines.getAndIncrement() == 0;
        }
    }

    public static void keyPeriod(DhLattice lattice, byte leafDetailLevel) {
        ToroidalShape shape = lattice.shape();
        String widthX = widthValue(shape, Direction.Axis.X);
        String widthZ = widthValue(shape, Direction.Axis.Z);
        if (!SEEN_KEY_PERIODS.add(widthX + ":" + widthZ + ":" + lattice.skewBlocks())) {
            return;
        }

        DhFold.Period period = DhFold.period(lattice, leafDetailLevel);
        LOGGER.info("[dh-compat] key_period width_x_blocks={} period_x_blocks={} laps_x={}"
                + " width_z_blocks={} period_z_blocks={} laps_z={} skew_blocks={} shift_x_blocks={}",
                widthX, periodValue(shape, Direction.Axis.X, period.xBlocks()),
                lapsValue(shape, Direction.Axis.X, period.xBlocks()),
                widthZ, periodValue(shape, Direction.Axis.Z, period.zBlocks()),
                lapsValue(shape, Direction.Axis.Z, period.zBlocks()),
                lattice.skewBlocks(), period.xShiftBlocks());
    }

    static String widthValue(ToroidalShape shape, Direction.Axis axis) {
        return shape.loops(axis) ? String.valueOf(shape.widthBlocks(axis)) : NONE;
    }

    static String periodValue(ToroidalShape shape, Direction.Axis axis, long periodBlocks) {
        return shape.loops(axis) ? String.valueOf(periodBlocks) : NONE;
    }

    static String lapsValue(ToroidalShape shape, Direction.Axis axis, long periodBlocks) {
        return shape.loops(axis) ? String.valueOf(periodBlocks / shape.widthBlocks(axis)) : NONE;
    }

    public static void repoShape(Object repo, IDhLevel level, boolean present) {
        LOGGER.info("[dh-compat] repo_shape repo={} level={} shape={}",
                repo.getClass().getSimpleName(), levelName(level), present ? "present" : "absent");
    }

    private static String levelName(IDhLevel level) {
        return level == null ? NONE : level.getLevelWrapper().getDhIdentifier();
    }

    static void sectionKeyFolded(long raw, long folded) {
        if (Key.SECTION.firstFold()) {
            LOGGER.info(foldedKeyLine(Key.SECTION, DhSectionPos.toString(raw), DhSectionPos.toString(folded)));
        }
    }

    static void chunkKeyFolded(int rawX, int rawZ, int foldedX, int foldedZ) {
        if (Key.CHUNK.firstFold()) {
            LOGGER.info(foldedKeyLine(Key.CHUNK, chunkValue(rawX, rawZ), chunkValue(foldedX, foldedZ)));
        }
    }

    static void beaconKeyFolded(DhBlockPos raw, DhBlockPos folded) {
        if (Key.BEACON.firstFold()) {
            LOGGER.info(foldedKeyLine(Key.BEACON, beaconValue(raw), beaconValue(folded)));
        }
    }

    static void keyKept(Key key) {
        key.kept.increment();
    }

    static String foldedKeyLine(Key key, String raw, String folded) {
        return "[dh-compat] folded_key key_type=" + key.label
                + " raw=" + raw + " folded=" + folded + " unchanged_keys=" + key.kept.sum();
    }

    static String chunkValue(int x, int z) {
        return x + "," + z;
    }

    static String beaconValue(DhBlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    static void seamSql(DhSeamSql.Site site, boolean matched) {
        if (SEEN_SEAM_SQL_SITES.add(site)) {
            LOGGER.info(seamSqlLine(site, matched));
        }
    }

    static String seamSqlLine(DhSeamSql.Site site, boolean matched) {
        return "[dh-compat] seam_sql site=" + site.label + " matched=" + matched;
    }

    static int foldedKeyLines(Key key) {
        return key.lines.get();
    }

    static long unchangedKeys(Key key) {
        return key.kept.sum();
    }

    static void resetKeyGates() {
        for (Key key : Key.values()) {
            key.lines.set(0);
            key.kept.reset();
        }
    }

    private DhProbes() {
    }
}
