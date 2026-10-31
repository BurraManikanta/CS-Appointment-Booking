package com.booking.service;

import com.booking.exception.ConflictException;
import com.booking.exception.ValidationException;
import com.booking.model.Appointment;
import com.booking.model.Status;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AppointmentBookingService {
    private final Map<String, List<Appointment>> activeByProvider = new HashMap<>();
    private final Map<String, Appointment> byId = new HashMap<>();
    private final Object lock = new Object();

    public Appointment book(String providerId, Instant start, Duration duration) {
        if (providerId == null || providerId.isBlank()) {
            throw new ValidationException("providerId must not be null or blank");
        }
        if (start == null) {
            throw new ValidationException("start must not be null");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new ValidationException("duration must be positive");
        }

        final Instant end;
        try {
            end = start.plus(duration);
        } catch (DateTimeException | ArithmeticException exception) {
            throw new ValidationException("appointment end is outside the supported time range", exception);
        }

        synchronized (lock) {
            List<Appointment> providerAppointments = activeByProvider.getOrDefault(providerId, List.of());
            for (Appointment existing : providerAppointments) {
                if (overlaps(start, end, existing.getStart(), existing.getEnd())) {
                    throw new ConflictException("appointment overlaps an active appointment for provider " + providerId);
                }
            }

            Appointment appointment = new Appointment(UUID.randomUUID().toString(), providerId, start, end);
            activeByProvider.computeIfAbsent(providerId, ignored -> new ArrayList<>()).add(appointment);
            byId.put(appointment.getId(), appointment);
            return appointment;
        }
    }

    public CancelResult cancel(String appointmentId) {
        if (appointmentId == null || appointmentId.isBlank()) {
            throw new ValidationException("appointmentId must not be null or blank");
        }

        synchronized (lock) {
            Appointment appointment = byId.get(appointmentId);
            if (appointment == null) {
                return CancelResult.NOT_FOUND;
            }
            if (appointment.getStatus() == Status.CANCELLED) {
                return CancelResult.ALREADY_CANCELLED;
            }

            appointment.markCancelled();
            List<Appointment> providerAppointments = activeByProvider.get(appointment.getProviderId());
            providerAppointments.remove(appointment);
            if (providerAppointments.isEmpty()) {
                activeByProvider.remove(appointment.getProviderId());
            }
            return CancelResult.CANCELLED;
        }
    }

    private boolean overlaps(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    int activeAppointmentCount(String providerId) {
        synchronized (lock) {
            return activeByProvider.getOrDefault(providerId, List.of()).size();
        }
    }
}
