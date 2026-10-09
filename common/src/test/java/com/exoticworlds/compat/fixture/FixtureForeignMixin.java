package com.exoticworlds.compat.fixture;

public class FixtureForeignMixin {
    @SuppressWarnings("ReturnValueIgnored")
    public void handler() {
        Integer.valueOf(1);
    }

    public void work() {
        Math.abs(1);
    }
}
