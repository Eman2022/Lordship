package io.github.lordship.tenancyterms;


public enum TenancyTermSource {
    LEASE, INCREASE_NOTICE, RULES_ADDENDUM, CORRECTION, MIGRATION;

    public boolean requiresInstrument() {
        return this != MIGRATION;
    }
}