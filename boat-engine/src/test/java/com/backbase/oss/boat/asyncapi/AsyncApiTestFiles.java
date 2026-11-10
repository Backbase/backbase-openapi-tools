package com.backbase.oss.boat.asyncapi;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class AsyncApiTestFiles {

    static final File SHARED_EVENTS_DIR =
        new File("../boat-trail-resources/src/main/resources/asyncapi/shared-events");
    static final File SHARED_EVENTS = new File(SHARED_EVENTS_DIR, "asyncapi.yaml");
    static final File SHARED_OPENAPI = new File(SHARED_EVENTS_DIR, "openapi.yaml");
    static final File CLOUD_EVENT = new File(
        "../boat-trail-resources/src/main/resources/asyncapi/cloud-event/events/adoption-decision-v1/asyncapi.yaml");
    static final File CLOUD_EVENTS = new File(
        "../boat-trail-resources/src/main/resources/asyncapi/cloud-events/adoption-events-v1.yaml");

    private AsyncApiTestFiles() {
    }

    static File write(Path dir, String name, String... lines) {
        try {
            Path file = dir.resolve(name);
            Files.createDirectories(file.getParent());
            Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            return file.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes an AsyncAPI 3.0 document with the given lines after the {@code info} object.
     */
    static File asyncApi(Path dir, String name, String... lines) {
        String[] all = new String[lines.length + 4];
        all[0] = "asyncapi: 3.0.0";
        all[1] = "info:";
        all[2] = "  title: Test";
        all[3] = "  version: 1.0.0";
        System.arraycopy(lines, 0, all, 4, lines.length);
        return write(dir, name, all);
    }
}
