package org.example.nonprofit;

import java.util.Objects;

public final class FailureTracker {
    private final InfraiClient client;
    public FailureTracker(InfraiClient client) { this.client = Objects.requireNonNull(client); }

    public <T> T record(String workflow, String itemId, Work<T> work) {
        try { return work.run(); }
        catch (RuntimeException failure) {
            client.capture(workflow + " failed for " + itemId + ": " + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            throw failure;
        }
    }

    @FunctionalInterface public interface Work<T> { T run(); }
}
