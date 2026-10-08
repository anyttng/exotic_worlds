package com.exoticworlds.compat.fixture;

public class FixtureTarget {
    public int counter;

    public void work() {
        helper(1);
        this.counter++;
        new StringBuilder();
    }

    public int helper(int value) {
        return value;
    }

    public static FixtureTarget create() {
        return new FixtureTarget();
    }
}
