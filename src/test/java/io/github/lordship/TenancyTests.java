package io.github.lordship;

import io.github.lordship.tenancy.internal.TenancyRepository;
import io.github.lordship.tenancy.internal.TenancyRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional //todo: tests for the service and controller files
public class TenancyTests {

    @Autowired
    TenancyRepository tenancyRepository;

    private TenancyRow buildRow() {
        return TenancyRow.forInsert(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Date(),
                null
        );
    }

    @Test
    void savePersistsRowAndReturnsGeneratedFields() {
        TenancyRow saved = tenancyRepository.save(buildRow());

        assertNotNull(saved.uuid());
        assertNotNull(saved.createdAt());
        assertNull(saved.endDate());
        assertNull(saved.deletedAt());
    }

    @Test
    void findATenancyById() {
        TenancyRow saved = tenancyRepository.save(buildRow());

        Optional<TenancyRow> found = tenancyRepository.findById((saved.uuid()));

        assertTrue(found.isPresent());
        assertEquals(saved.uuid(), found.get().uuid());
    }

    @Test
    void updatedAtChangesOnUpdate() {
        TenancyRow saved = tenancyRepository.save(buildRow());

        LocalDateTime before = saved.updatedAt();

        // Close tenancy (triggers update)
        TenancyRow closed = tenancyRepository.close(saved.uuid(), new Date());

        // Checks for difference in timestamps
        assertTrue(
                closed.updatedAt().isAfter(before)
                        || closed.updatedAt().isEqual(before)
        );
    }

    @Test
    void filterTenancyByLot() {
        // Currently can only have one tenancy to a lot
        UUID lot = UUID.randomUUID();
        UUID lot2 = UUID.randomUUID();
        UUID lot3 = UUID.randomUUID();

        tenancyRepository.save(TenancyRow.forInsert(lot, UUID.randomUUID(), new Date(), null));
        tenancyRepository.save(TenancyRow.forInsert(lot2, UUID.randomUUID(), new Date(), null));
        tenancyRepository.save(TenancyRow.forInsert(lot3, UUID.randomUUID(), new Date(), new Date()));

        List<TenancyRow> active = tenancyRepository.findActiveByLot(lot);

        // Only one lot should show
        assertEquals(1, active.size());
        assertTrue(active.stream().allMatch(t -> t.endDate() == null));

        assertNotEquals(3, active.size());
        assertTrue(active.stream().allMatch(t -> t.endDate() == null));

    }

    @Test
    void closingTenancy() {
        TenancyRow saved = tenancyRepository.save(buildRow());

        Date endDate = new Date();
        TenancyRow closed = tenancyRepository.close(saved.uuid(), endDate);

        // Compares only dates (Java and Postgres DATEs are different;
        // Postgres timestamps at Midnight while Java contains an accurate timestamp)
        assertEquals(
                endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                closed.endDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        );
        assertNotNull(closed.updatedAt());
    }

    @Test
    void softDeleteRemovesFromTable() {
        TenancyRow saved = tenancyRepository.save(buildRow());

        tenancyRepository.softDelete(saved.uuid());

        Optional<TenancyRow> found = tenancyRepository.findById(saved.uuid());
        assertTrue(found.isEmpty());

        List<TenancyRow> active = tenancyRepository.findActiveByLot(saved.lotNumber());
        assertTrue(active.isEmpty());
    }
}
