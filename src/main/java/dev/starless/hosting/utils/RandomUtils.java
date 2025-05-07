package dev.starless.hosting.utils;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;

@UtilityClass
public class RandomUtils {

    private final SecureRandom random = new SecureRandom();
    private final String ALPHABET = "qwertyuiopasdfghjklzxcvbnm";

    public String randomString(int length) {
        final String charsPool = ALPHABET.concat(ALPHABET.toUpperCase());
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charsPool.charAt(random.nextInt(charsPool.length())));
        }
        return sb.toString();
    }
}
