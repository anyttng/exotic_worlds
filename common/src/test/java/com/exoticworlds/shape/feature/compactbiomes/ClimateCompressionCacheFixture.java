package com.exoticworlds.shape.feature.compactbiomes;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ClimateCompressionCache;
import com.exoticworlds.shape.feature.compactbiomes.ClimateCompression.Resolved;

final class ClimateCompressionCacheFixture {
    static final class Storing implements ClimateCompressionCache {
        private @Nullable Resolved resolved;
        int stores;

        @Override
        public @Nullable Resolved exotic_worlds$getClimateCompression() {
            return this.resolved;
        }

        @Override
        public void exotic_worlds$setClimateCompression(Resolved resolved) {
            this.resolved = resolved;
            this.stores++;
        }
    }

    private ClimateCompressionCacheFixture() {
    }
}
