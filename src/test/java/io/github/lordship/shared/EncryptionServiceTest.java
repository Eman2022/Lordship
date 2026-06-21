package io.github.lordship.shared;

import org.junit.jupiter.api.Test;
import java.security.SecureRandom;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncryptionServiceTest {

    @Test
    public void encryptionSanityCheck(){
        String toEncrypt = "10_liTtlE   dAnc1ng monkies";
        String keys = generateBase64Key() + "," + generateBase64Key() + "," + generateBase64Key();

        EncryptionProperties encryptionProperties = new EncryptionProperties("AES/GCM/NoPadding", keys);

        EncryptionService encryptionService = new EncryptionService(encryptionProperties);

        String valueEncrypted = encryptionService.encrypt(toEncrypt);
        String decrypted = encryptionService.decrypt(valueEncrypted);

        //important to remember: keys start with a number to
        String prefix = valueEncrypted.substring(0, valueEncrypted.indexOf(':'));
        int keyNumber = Integer.parseInt(prefix);
        assertTrue(keyNumber >= 1 && keyNumber <= 3);
        assertEquals(toEncrypt, decrypted);
    }


private String generateBase64Key() {
    byte[] keyBytes = new byte[32];
    new SecureRandom().nextBytes(keyBytes);
    return Base64.getEncoder().encodeToString(keyBytes);
}
}


