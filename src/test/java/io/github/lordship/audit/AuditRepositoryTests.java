package io.github.lordship.audit;

import io.github.lordship.IntegrationTest;
import io.github.lordship.audit.internal.AuditLogRow;
import io.github.lordship.shared.PageRequest;
import io.github.lordship.shared.PageResult;
import io.github.lordship.shared.UserType;
import io.github.lordship.audit.internal.AuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Transactional
public class AuditRepositoryTests extends IntegrationTest {

    @Autowired
    AuditRepository auditRepository;

    @Test
    void shouldSaveAuditRow_whenValidRowIsProvided(){
        // Arrange
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

        // Act
        AuditLogRow savedRow = auditRepository.save(row);

        // Assert
        assertThat(savedRow.uuid()).isNotNull();
        assertThat(savedRow.userType()).isEqualTo(UserType.AGENT);
        assertThat(savedRow.operation()).isEqualTo(OperationType.INSERT);
        assertThat(savedRow.changedAt()).isNotNull();
    }

    @Test
    void findAllAgentAuditLogs_shouldReturnPagedAuditLogsInRequestedOrder_whenAgentHasMultipleLogs() {
        // Arrange
          // we'll make three logs showing
        UUID agentId = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            auditRepository.save(new AuditLogRow(
                    null,
                    UUID.randomUUID(),
                    agentId,
                    UserType.AGENT,
                    "127.0.0.1",
                    "agent",
                    UUID.randomUUID().toString(),
                    OperationType.UPDATE,
                    null,
                    "{\"work_email\":\"test" + i + "@test.com\"}",
                    null
            ));
        }

        // Act
        PageRequest pageRequest = PageRequest.of(0, 2, "changed_at", false);
        PageResult<AuditLog> result = auditRepository.findAllAgentAuditLogs(agentId, pageRequest);

        // Assert
        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(0);
        // descending by changed_at: most recently inserted row comes first
        assertThat(result.content().get(0).changedAt())
                .isAfterOrEqualTo(result.content().get(1).changedAt());
    }

    @Test
    void findAllAgentAuditLogs_shouldThrowException_whenSortColumnIsUnknown() {
        UUID agentId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 10, "value_after", true);

        assertThatThrownBy(() -> auditRepository.findAllAgentAuditLogs(agentId, pageRequest))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }
}
