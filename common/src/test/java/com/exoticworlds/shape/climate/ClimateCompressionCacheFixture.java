package com.exoticworlds.shape.climate;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ClimateCompressionCache;
import com.exoticworlds.shape.climate.ClimateCompression.Resolved;

final class ClimateCompressionCacheFixture {
    static final class Storing implements ClimateCompressionCache {
        private @Nullable Resolved resolved;
        int stores;

        @Override
        public @Nullable Resolved toroidal$climateCompression() {
            return this.resolved;
        }

        @Override
        public void toroidal$climateCompression(Resolved resolved) {
            this.resolved = resolved;
            this.stores++;
        }
    }

    private ClimateCompressionCacheFixture() {
    }
}
