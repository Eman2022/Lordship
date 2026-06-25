package io.github.lordship.lots.internal;

import io.github.lordship.lots.Lot;
import io.github.lordship.shared.EncryptionProperties;
import io.github.lordship.shared.EncryptionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LotRowEncryptionTest {

    private final EncryptionService encryptionService = new EncryptionService(
            new EncryptionProperties(
                    "AES/GCM/NoPadding",
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
            )
    );

    @Test
    void toLotDecryptsEncryptedNotes() {
        String notes = "Front row";
        String encryptedNotes = encryptionService.encrypt(notes);
        LotRow row = new LotRow(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "12",
                "REN",
                "Rental lot",
                encryptedNotes,
                1,
                LocalDateTime.now(),
                null
        );

        Lot lot = row.toLot(encryptionService);

        assertNotEquals(notes, encryptedNotes);
        assertEquals(notes, lot.notes());
    }
}
