package io.github.lordship.access.internal;



import java.time.OffsetDateTime;
import java.util.UUID;

public record LoginEventRow(
        UUID uuid,
        UUID agentId,
        OffsetDateTime occurredAt,
        String ipAddress,
        String browserClient,
        String browserOs,
        int outcome
) {

    public LoginEventRow (UUID agentId, OffsetDateTime occurredAt, String ipAddress, String browserClient, String browserOs,int outcome) {
        this (
                null,
                agentId,
                occurredAt,
                ipAddress,
                browserClient,
                browserOs,
                outcome
        );
    }
}
