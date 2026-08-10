package io.github.lordship.chargeterms;

public enum ChargeTermStatus {
    PROPOSED, PENDING, ACTIVE, CANCELLED;
    // PROPOSED  - editable. Nothing has been generated yet.
    // PENDING   - a document exists and is out for signature/service. No longer editable.
    // ACTIVE    - when the valid_at date starts (and when a charge term is set to active) the charge term can be used
    // CANCELLED - was in use but was canceled

    public boolean isEditable() {
        return this == PROPOSED;
    }

    public boolean isDeletable() {
        return this == PROPOSED || this == PENDING;
    }
}
