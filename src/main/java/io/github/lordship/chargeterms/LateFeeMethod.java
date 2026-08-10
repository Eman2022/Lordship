package io.github.lordship.chargeterms;

public enum LateFeeMethod {
    FLAT, PERCENT, DAILY;

    // note: late_fee_max only applies when it's not a flat amt
    public boolean supportsMax() {
        return this != FLAT;
    }
}
