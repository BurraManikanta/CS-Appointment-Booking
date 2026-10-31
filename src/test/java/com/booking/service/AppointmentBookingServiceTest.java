package com.booking.service;

import com.booking.exception.ConflictException;
import com.booking.exception.ValidationException;
import com.booking.model.Appointment;
import com.booking.model.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppointmentBookingServiceTest {
    private static final Instant START = Instant.parse("2026-01-15T10:00:00Z");
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private AppointmentBookingService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentBookingService();
    }

    @Test
    void booksAppointmentWithUtcTimesAndPositiveDuration() {
        Appointment appointment = service.book("provider-1", START, ONE_HOUR);

        assertNotNull(appointment.getId());
        assertEquals("provider-1", appointment.getProviderId());
        assertEquals(START, appointment.getStart());
        assertEquals(START.plus(ONE_HOUR), appointment.getEnd());
        assertEquals(Status.ACTIVE, appointment.getStatus());
        assertEquals(1, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void rejectsOverlappingAppointmentForSameProviderWithoutChangingStore() {
        service.book("provider-1", START, ONE_HOUR);

        assertThrows(ConflictException.class,
                () -> service.book("provider-1", START.plus(Duration.ofMinutes(30)), ONE_HOUR));

        assertEquals(1, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void allowsAdjacentAppointments() {
        service.book("provider-1", START, ONE_HOUR);

        Appointment adjacent = service.book("provider-1", START.plus(ONE_HOUR), ONE_HOUR);

        assertEquals(START.plus(ONE_HOUR), adjacent.getStart());
        assertEquals(2, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void differentProvidersCanBookSameTime() {
        service.book("provider-1", START, ONE_HOUR);

        Appointment secondProviderAppointment = service.book("provider-2", START, ONE_HOUR);

        assertEquals("provider-2", secondProviderAppointment.getProviderId());
        assertEquals(1, service.activeAppointmentCount("provider-1"));
        assertEquals(1, service.activeAppointmentCount("provider-2"));
    }

    @Test
    void cancellingAppointmentFreesSlotImmediately() {
        Appointment original = service.book("provider-1", START, ONE_HOUR);

        assertEquals(CancelResult.CANCELLED, service.cancel(original.getId()));
        assertEquals(Status.CANCELLED, original.getStatus());
        assertEquals(0, service.activeAppointmentCount("provider-1"));

        Appointment replacement = service.book("provider-1", START, ONE_HOUR);
        assertEquals(START, replacement.getStart());
        assertEquals(1, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void cancellingMissingAppointmentReturnsNotFound() {
        assertEquals(CancelResult.NOT_FOUND, service.cancel("missing-id"));
        assertEquals(0, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void cancellingAlreadyCancelledAppointmentReturnsClearResult() {
        Appointment appointment = service.book("provider-1", START, ONE_HOUR);
        assertEquals(CancelResult.CANCELLED, service.cancel(appointment.getId()));

        assertEquals(CancelResult.ALREADY_CANCELLED, service.cancel(appointment.getId()));
        assertEquals(0, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void rejectsNullOrBlankProviderWithoutChangingStore() {
        assertThrows(ValidationException.class, () -> service.book(null, START, ONE_HOUR));
        assertThrows(ValidationException.class, () -> service.book(" ", START, ONE_HOUR));

        assertEquals(0, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void rejectsNullStartWithoutChangingStore() {
        assertThrows(ValidationException.class, () -> service.book("provider-1", null, ONE_HOUR));

        assertEquals(0, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void rejectsNullZeroAndNegativeDurationWithoutChangingStore() {
        assertThrows(ValidationException.class, () -> service.book("provider-1", START, null));
        assertThrows(ValidationException.class, () -> service.book("provider-1", START, Duration.ZERO));
        assertThrows(ValidationException.class,
                () -> service.book("provider-1", START, Duration.ofSeconds(-1)));

        assertEquals(0, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void rejectsEndTimeOverflowWithoutChangingStore() {
        assertThrows(ValidationException.class,
                () -> service.book("provider-1", Instant.MAX, Duration.ofSeconds(1)));

        assertEquals(0, service.activeAppointmentCount("provider-1"));
    }

    @Test
    void rejectsNullOrBlankAppointmentIdWithoutChangingStore() {
        service.book("provider-1", START, ONE_HOUR);

        assertThrows(ValidationException.class, () -> service.cancel(null));
        assertThrows(ValidationException.class, () -> service.cancel(" "));

        assertEquals(1, service.activeAppointmentCount("provider-1"));
    }
}
