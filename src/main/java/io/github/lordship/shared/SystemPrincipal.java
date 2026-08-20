package io.github.lordship.shared;

import java.util.UUID;


public final class SystemPrincipal {

    public static final UUID PERSON_UUID = UUID.fromString("00000000-0000-7000-8000-000000000001");
    public static final UUID AGENT_UUID = UUID.fromString("00000000-0000-7000-8000-000000000002");

    private SystemPrincipal() {
    }
}