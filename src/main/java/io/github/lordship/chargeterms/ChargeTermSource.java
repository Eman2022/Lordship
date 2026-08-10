package io.github.lordship.chargeterms;

public enum ChargeTermSource {
    LEASE, INCREASE_NOTICE, RULES_ADDENDUM, CORRECTION, MIGRATION;

    public boolean requiresInstrument() {
        return this != MIGRATION;
    }
}