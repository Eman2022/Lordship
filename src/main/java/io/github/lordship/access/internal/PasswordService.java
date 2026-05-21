package io.github.lordship.access.internal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public PasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String plaintext) {
        return passwordEncoder.encode(plaintext);
    }

    public boolean verify(String plaintext, String hash) {
        return passwordEncoder.matches(plaintext, hash);
    }

}
