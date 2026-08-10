package io.github.lordship.chargeterms;

public enum UtilityMethod {
    NONE, FLAT, RUBS, SUBMETERED;

    public boolean requiresFlatAmount() {
        return this == FLAT;
    }

    public boolean requiresMeter() {
        return this == SUBMETERED;
    }
}