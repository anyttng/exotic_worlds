package com.exoticworlds.api.v2.feature;

import java.util.List;

/**
 * The moment the starts of the dimension's structure sets are settled, once per dimension. A feature adds starts the
 * game's own placement did not give.
 */
public interface StructureStartsMomentV2 {
    /**
     * Called once for each structure set of the dimension.
     *
     * @return the starts to add to this set. An added start the set cannot take is dropped and logged
     */
    List<StructureStartsV2.Added> atStructureStarts(StructureStartsV2 starts);
}
