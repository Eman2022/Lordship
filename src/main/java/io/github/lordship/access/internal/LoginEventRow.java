package io.github.lordship.access.internal;


import java.time.LocalDateTime;
import java.util.UUID;

public record LoginEventRow(
        UUID uuid,
        UUID agentId,
        LocalDateTime occurredAt,
        String ipAddress,
        String browserClient,
        String browserOs,
        int outcome
) {

    public LoginEventRow (UUID agentId, LocalDateTime occurredAt, String ipAddress, String browserClient, String browserOs,int outcome) {
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
