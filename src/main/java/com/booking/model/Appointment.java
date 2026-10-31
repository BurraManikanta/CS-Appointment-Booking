package com.booking.model;

import java.time.Instant;
import java.util.Objects;

public final class Appointment {
    private final String id;
    private final String providerId;
    private final Instant start;
    private final Instant end;
    private Status status;

    public Appointment(String id, String providerId, Instant start, Instant end) {
        this.id = Objects.requireNonNull(id, "id");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
        this.status = Status.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }

    public Status getStatus() {
        return status;
    }

    public void markCancelled() {
        status = Status.CANCELLED;
    }
}
