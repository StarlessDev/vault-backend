package dev.starless.hosting.objects;

public record EncryptedFile(byte[] content, String iv, String salt) {
}
