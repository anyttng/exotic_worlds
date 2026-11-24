package com.exoticworlds.engine.noise.check;

import java.util.List;

import org.jspecify.annotations.Nullable;

public sealed interface SeamCause {
    String getKey();

    record Node(String type, @Nullable String holder, @Nullable String namespace, boolean isVanilla,
            List<String> candidates) implements SeamCause {
        private static final String KEY_PREFIX = "node:";

        @Override
        public String getKey() {
            return KEY_PREFIX + this.type;
        }
    }

    record Biomes(String type, @Nullable String namespace) implements SeamCause {
        private static final String KEY_PREFIX = "biome_source:";

        @Override
        public String getKey() {
            return KEY_PREFIX + this.type;
        }
    }

    record Generator(String type, @Nullable String namespace) implements SeamCause {
        private static final String KEY_PREFIX = "generator:";

        @Override
        public String getKey() {
            return KEY_PREFIX + this.type;
        }
    }

    record Follows(String field) implements SeamCause {
        private static final String KEY_PREFIX = "follows:";

        @Override
        public String getKey() {
            return KEY_PREFIX + this.field;
        }
    }

    record NotFound() implements SeamCause {
        private static final String KEY = "not_found";

        @Override
        public String getKey() {
            return KEY;
        }
    }
}
