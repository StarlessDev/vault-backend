package dev.starless.hosting;

import dev.starless.hosting.objects.EncryptedFile;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.HexFormat;

public class EncryptionEngine {

    private static final String ALGORITHM = "AES";
    private static final String MODE = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int GCM_IV_LENGTH_BYTES = 12; // 96 bits
    private static final int GCM_TAG_LENGTH_BITS = 128; // Common tag length
    private static final int PBKDF2_ITERATIONS = 65536; // Increase for more security
    private static final int SALT_LENGTH_BYTES = 16;

    public EncryptedFile encrypt(final byte[] bytes,
                                  final String password)
            throws NoSuchAlgorithmException,
            InvalidKeySpecException,
            NoSuchPaddingException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException {
        final SecureRandom random = new SecureRandom();

        // 1. Generate Salt for PBKDF2
        final byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);

        // 2. Derive Key using PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);

        // 3. Generate IV for GCM
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        random.nextBytes(iv);

        // 4. Encrypt
        Cipher cipher = Cipher.getInstance(MODE);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        final byte[] ciphertext = cipher.doFinal(bytes);

        return new EncryptedFile(
                ciphertext,
                HexFormat.of().formatHex(iv),
                HexFormat.of().formatHex(salt)
        );
    }
}
