package io.github.lordship.shared;

public enum FeeMethod {
    NONE, FLAT;

    public boolean requiresAmount() {
        return this == FLAT;
    }
}