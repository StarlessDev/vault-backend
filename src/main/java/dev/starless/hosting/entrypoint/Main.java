package dev.starless.hosting.entrypoint;

import dev.starless.hosting.Backend;

public class Main {

    public static void main(String[] args) {
        final Backend backend = new Backend();
        backend.init();
        Runtime.getRuntime().addShutdownHook(new Thread(backend::shutdown));
    }
}
