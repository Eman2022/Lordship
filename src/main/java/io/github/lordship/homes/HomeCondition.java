package io.github.lordship.homes;

// Declaration order is the scale, best to worst -- sort and compare on it rather
// than coding the values in the database. DEMO is a decision, not a condition,
// and deliberately competes with the words that imply a usable building.
public enum HomeCondition {
    NEW,
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNINHABITABLE,
    DEMO
}
