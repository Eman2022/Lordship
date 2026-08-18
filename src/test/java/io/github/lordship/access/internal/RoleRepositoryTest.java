package io.github.lordship.access.internal;

import io.github.lordship.IntegrationTest;
import io.github.lordship.access.internal.rbac.RoleRepository;
import io.github.lordship.access.internal.rbac.RoleRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class RoleRepositoryTest extends IntegrationTest {

    @Autowired
    RoleRepository roleRepository;

    private RoleRow buildRow() { return new RoleRow("Role name", "A test role"); }

    @Test
    void save_shouldPersistRow_andReturnGeneratedFields() {
        // Arrange
        RoleRow role = buildRow();

        // Act
        RoleRow savedRow = roleRepository.save(role);
        Duration age = Duration.between(savedRow.createdAt(), OffsetDateTime.now(ZoneOffset.UTC));

        // Assert
        assertNotNull(savedRow.uuid());
        assertEquals("Role name", role.roleName());
        assertNull(role.deletedAt());
        assertNotNull(savedRow.createdAt());
        assertEquals("A test role", savedRow.roleDescription());
        assertTrue(age.toSeconds() < 5, "expecting to be created within 5s");
    }

    @Test
    void save_shouldThrow_whenSavingExistingRoleName() {
        // Arrange
        RoleRow role = buildRow();
        RoleRow role2 = buildRow();
        roleRepository.save(role);

        // Act & Assert
        assertThrows(DuplicateKeyException.class, () -> roleRepository.save(role2));
    }

    @Test
    void patch_shouldPersistRow_whenAllFieldsChanged() {
        // Arrange
        RoleRow role = buildRow();
        RoleRow savedRow = roleRepository.save(role);

        // Act
        Optional<RoleRow> patched = roleRepository.patch(savedRow.uuid(), Map.of("role_name", "New role name", "role_description", "New role description"));

        // Assert
        assertTrue(patched.isPresent());
        assertEquals("New role name", patched.get().roleName());
        assertEquals("New role description", patched.get().roleDescription());
    }

}
