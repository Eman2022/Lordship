package io.github.lordship.shared;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class EncryptionService {

    private static final int GCM_IV_LENGTH  = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final String algorithm;
    private final Map<Integer, SecretKeySpec> keys;

    public EncryptionService(EncryptionProperties encryptionProperties)
    {
        this.algorithm = encryptionProperties.algorithm();
        this.keys = new HashMap<>();

        encryptionProperties.keyMap().forEach((number, base64key) -> {
            byte[] keyBytes = Base64.getDecoder().decode(base64key);
            this.keys.put(number, new SecretKeySpec(keyBytes, "AES"));
        });
    }

    public String encrypt(String plainText) {
        try {
            int keyNumber = currentKeyNumber();
            SecretKeySpec key = keys.get(keyNumber);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes());

            // prepend IV to ciphertext so we can recover it on decrypt
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            String base64Body = Base64.getEncoder().encodeToString(combined);
            return keyNumber + ":" + base64Body;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        try {
            int separatorIndex = stored.indexOf(':');
            if (separatorIndex == -1) {
                throw new IllegalArgumentException("Stored value is missing key version");
            }

            int keyNumber = Integer.parseInt(stored.substring(0, separatorIndex));
            SecretKeySpec key = keys.get(keyNumber);

            if (key == null){
                throw new IllegalStateException("No key loaded for version " + keyNumber);
            }

            String base64CipherText = stored.substring(separatorIndex + 1);
            byte[] combined = Base64.getDecoder().decode(base64CipherText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText));
        }  catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private int currentKeyNumber() {
        YearMonth now = YearMonth.now();
        int monthIndex = now.getYear() * 12 + now.getMonthValue();
        return (monthIndex % keys.size()) + 1;
    }
}
