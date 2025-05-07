package dev.starless.hosting.webserver.jwt;

import lombok.AccessLevel;
import lombok.Getter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.Function;

@Getter
public class JWTKeys {

    private static final int RSA_KEY_SIZE = 4096;
    private static final String PRIVATE_KEY_FILE = "private.key";
    private static final String PUBLIC_KEY_FILE = "public.key";

    @Getter(AccessLevel.NONE)
    private final File directory;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    public JWTKeys() {
        this.directory = new File("keys");

        this.privateKey = null;
        this.publicKey = null;
    }

    public void load() {
        final boolean doesDirExist = directory.isDirectory();
        if (!doesDirExist && !directory.mkdirs()) {
            throw new RuntimeException("Could not create directory: " + directory.getAbsolutePath());
        }

        if (doesDirExist) {
            this.publicKey = this.loadKey(PUBLIC_KEY_FILE,
                    X509EncodedKeySpec::new,
                    keySpec -> {
                        try {
                            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
                        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                            throw new RuntimeException(e);
                        }
                    });

            this.privateKey = this.loadKey(PRIVATE_KEY_FILE,
                    PKCS8EncodedKeySpec::new,
                    keySpec -> {
                        try {
                            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
                        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } else {
            final KeyPair keyPair = this.createKeyPair();
            this.createKeyPair();

            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();

            this.saveKeypair(PUBLIC_KEY_FILE, publicKey);
            this.saveKeypair(PRIVATE_KEY_FILE, privateKey);
        }
    }

    private KeyPair createKeyPair() {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        generator.initialize(RSA_KEY_SIZE);
        return generator.generateKeyPair();
    }

    private <T extends RSAKey> T loadKey(final String fileName,
                                         final Function<byte[], ? extends EncodedKeySpec> keySpecImpl,
                                         final Function<EncodedKeySpec, T> generator) {
        final File file = new File(this.directory, fileName);
        if (!file.exists()) {
            throw new RuntimeException("Key does not exist: " + file.getAbsolutePath());
        }

        byte[] keyBytes;
        try {
            keyBytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final EncodedKeySpec keySpec = keySpecImpl.apply(keyBytes);
        return generator.apply(keySpec);
    }

    private void saveKeypair(final String fileName,
                             final Key key) {
        try (FileOutputStream fos = new FileOutputStream(new File(directory, fileName))) {
            fos.write(key.getEncoded());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isLoaded() {
        return privateKey != null && publicKey != null;
    }
}
