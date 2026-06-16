package com.jiber.backend.publicdata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class FixtureText {

    private FixtureText() {
    }

    static String read(String path) {
        try (var stream = FixtureText.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Fixture not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Fixture read failed: " + path, exception);
        }
    }
}
