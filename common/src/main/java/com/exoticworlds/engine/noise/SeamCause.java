package com.exoticworlds.engine.noise;

import java.util.List;

import org.jspecify.annotations.Nullable;

public sealed interface SeamCause {
    String key();

    record Node(String type, @Nullable String holder, @Nullable String namespace, boolean vanilla,
            List<String> candidates) implements SeamCause {
        private static final String KEY_PREFIX = "node:";

        @Override
        public String key() {
            return KEY_PREFIX + this.type;
        }
    }

    record Biomes(String type, @Nullable String namespace) implements SeamCause {
        private static final String KEY_PREFIX = "biome_source:";

        @Override
        public String key() {
            return KEY_PREFIX + this.type;
        }
    }

    record Generator(String type, @Nullable String namespace) implements SeamCause {
        private static final String KEY_PREFIX = "generator:";

        @Override
        public String key() {
            return KEY_PREFIX + this.type;
        }
    }

    record Follows(String field) implements SeamCause {
        private static final String KEY_PREFIX = "follows:";

        @Override
        public String key() {
            return KEY_PREFIX + this.field;
        }
    }

    record NotFound() implements SeamCause {
        private static final String KEY = "not_found";

        @Override
        public String key() {
            return KEY;
        }
    }
}
