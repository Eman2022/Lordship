package io.github.lordship.shared;

public class InvalidEncryptionConfigException extends RuntimeException {
    public InvalidEncryptionConfigException(String message) {
        super(message);
    }

    public InvalidEncryptionConfigException(String message, Throwable cause){
        super(message, cause);
    }
}
