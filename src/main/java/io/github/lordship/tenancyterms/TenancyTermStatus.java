package io.github.lordship.tenancyterms;

public enum TenancyTermStatus {
    PROPOSED, PENDING, ACTIVE, CANCELLED;
    // PROPOSED  - editable, filled in incrementally. Nothing generated yet.
    // PENDING   - a document is out for signature or service. No longer editable.
    // ACTIVE    - in force from valid_at until a later ACTIVE term supersedes it.
    // CANCELLED - was in force, retracted; excluded from resolution entirely.

    public boolean isEditable() {
        return this == PROPOSED;
    }

    public boolean isDeletable() {
        return this == PROPOSED || this == PENDING;
    }
}