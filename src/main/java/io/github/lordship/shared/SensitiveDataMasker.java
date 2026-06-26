package io.github.lordship.shared;

public final class SensitiveDataMasker {

    private SensitiveDataMasker() {}

    public static String defaultMask(String value) {
        if (value == null) return null;
        if (value.isEmpty()) return value;
        return value.replaceAll("[a-zA-Z0-9]", "*");
    }

    public static String maskSsn(String social) {
        if (social == null) return null;
        if (social.isEmpty()) return social;
        return "***-**-" + social.substring(social.length() - 4);
    }

    // add more masking rules as needed
}