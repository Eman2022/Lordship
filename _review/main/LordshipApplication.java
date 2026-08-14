package io.github.lordship;

import io.github.lordship.shared.EncryptionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(EncryptionProperties.class)
public class LordshipApplication {

    public static void main(String[] args) {
        SpringApplication.run(LordshipApplication.class, args);
    }

}