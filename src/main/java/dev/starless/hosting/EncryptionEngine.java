package dev.starless.hosting;

import dev.starless.hosting.objects.EncryptionDetails;
import dev.starless.hosting.utils.RandomUtils;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private static final int KEY_LENGTH = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int GCM_IV_LENGTH_BYTES = 12; // 96 bits
    public static final int GCM_TAG_LENGTH_BITS = 128; // Common tag length
    private static final int PBKDF2_ITERATIONS = 65536; // Increase for more security
    private static final int SALT_LENGTH_BYTES = 16;

    public EncryptionDetails encrypt(final InputStream is, final OutputStream os)
            throws NoSuchAlgorithmException,
            InvalidKeySpecException,
            NoSuchPaddingException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException,
            IOException {
        final SecureRandom random = new SecureRandom();
        final HexFormat hexFormat = HexFormat.of();

        // 1. Generate Salt for PBKDF2
        final byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);

        // 2. Derive Key using PBKDF2
        final String pwd = RandomUtils.randomString(KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);

        // 3. Generate IV for GCM
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        random.nextBytes(iv);

        // 4. Encrypt
        Cipher cipher = Cipher.getInstance(MODE);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        try (final CipherOutputStream cos = new CipherOutputStream(os, cipher)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        }

        return new EncryptionDetails(pwd, hexFormat.formatHex(iv) + hexFormat.formatHex(salt));
    }

    public CipherInputStream decrypt(final InputStream is, final String key) throws NoSuchAlgorithmException,
            InvalidKeySpecException,
            NoSuchPaddingException,
            InvalidAlgorithmParameterException,
            InvalidKeyException {
        if (key.length() != KEY_LENGTH + (GCM_IV_LENGTH_BYTES + SALT_LENGTH_BYTES) * 2) return null;
        // 1. Decode Key, Salt and IV
        final String pwd = key.substring(0, KEY_LENGTH);

        final HexFormat format = HexFormat.of();
        final String ivAndSalt = key.substring(KEY_LENGTH);
        final byte[] iv = format.parseHex(ivAndSalt.substring(0, GCM_IV_LENGTH_BYTES * 2));
        final byte[] salt = format.parseHex(ivAndSalt.substring(GCM_IV_LENGTH_BYTES * 2));
        // 2. Derive Key using PBKDF2 (same parameters as encryption)
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);

        // 3. Decrypt
        Cipher cipher = Cipher.getInstance(MODE);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        // doFinal will decrypt and verify the GCM authentication tag simultaneously.
        // If the tag is invalid, it will throw an AEADBadTagException (a subclass of BadPaddingException).
        return new CipherInputStream(is, cipher);
    }
}
