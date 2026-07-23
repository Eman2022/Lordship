package io.github.lordship.shared;


import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.Cipher;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "lordship.encryption")
public record EncryptionProperties (
        String algorithm,
        String keys
){

    public EncryptionProperties {
        if(algorithm == null || algorithm.isBlank()){
            throw new InvalidEncryptionConfigException("Algorithm missing from EncryptionProperties");
        }

        try{
            Cipher.getInstance(algorithm);
        } catch (Exception e) {
            throw new InvalidEncryptionConfigException("Algorithm " + algorithm + " is not valid. ", e);
        }

        if (keys == null || keys.isBlank()){
            throw new InvalidEncryptionConfigException("Encryption keys not found");
        }

        if (keys.charAt(keys.length() - 1) == ',') {
            throw new InvalidEncryptionConfigException("Encryption keys contain incorrect trailing comma");
        }

        String[] keyParts  = keys.split(",");

        for (int i = 0; i < keyParts.length; i++) {
            int number = i + 1;
            String key = keyParts[i].trim();

            if (key.isBlank()) {
                throw new InvalidEncryptionConfigException("Key number " + number + " is missing");
            }

            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(key);
            } catch (IllegalArgumentException e) {
                throw new InvalidEncryptionConfigException("Key number " + number + " is not valid Base64", e);
            }

            if (decoded.length != 32) {
                throw new InvalidEncryptionConfigException("Key number " + number + " is not of length 32");
            }
        }
    }
    public Map<Integer, String> keyMap(){
        String[] parts = keys.split(",");
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < parts.length; i++){
            map.put(i + 1, parts[i].trim());
        }
        return map;
    }
}