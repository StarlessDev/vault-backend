package dev.starless.hosting.objects;

import java.time.Duration;

public record Token(String token, Duration expiry) {
}
