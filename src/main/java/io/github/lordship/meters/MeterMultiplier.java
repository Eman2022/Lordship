package io.github.lordship.meters;

public enum MeterMultiplier {
    GAL(1.0),
    TEN_GAL(10.0),
    HUNDRED_GAL(100.0),
    CBF(7.48), // Cubic Feet needs to be converted to gallons
    TEN_CBF(74.8),
    HUNDRED_CBF(748.0),
    KWH(1.0),
    TEN_KWH(10.0),
    HUNDRED_KWH(100.0);

    public final double multiplier;

    MeterMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }
}
