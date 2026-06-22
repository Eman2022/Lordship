package io.github.lordship.audit;

import io.github.lordship.shared.UserType;
import io.github.lordship.audit.internal.AuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuditRepositoryTests {


    @Autowired
    AuditRepository auditRepository;

    @Test
    void savesAuditRow(){
        AuditLogRow row = new AuditLogRow(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UserType.AGENT,
                "127.0.0.1",
                "agent",
                UUID.randomUUID().toString(),
                OperationType.INSERT,
                null,
                "{\"work_email\":\"test@test.com\"}",
                null
        );

        AuditLogRow savedRow = auditRepository.save(row);

        assertThat(savedRow.uuid()).isNotNull();
        assertThat(savedRow.userType()).isEqualTo(UserType.AGENT);
        assertThat(savedRow.operation()).isEqualTo(OperationType.INSERT);
        assertThat(savedRow.changedAt()).isNotNull();
    }
}
