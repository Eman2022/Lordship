package io.github.lordship.shared;

public enum FeeMethod {
    NONE, FLAT, PERCENT_OF_RENT, BANK_OR_FLAT;
    // bank_or_flat: either the amt charged to us by the bank or a flat amt, whichever is greater

    public boolean requiresAmount() {
        return this == FLAT;
    }
}