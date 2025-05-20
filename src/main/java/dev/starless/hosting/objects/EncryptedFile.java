package dev.starless.hosting.objects;

public record EncryptedFile(byte[] content, String key, String ivAndSalt) {
}
