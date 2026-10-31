# Design specification

## Service boundary

`AppointmentBookingService` is the booking boundary. A small JDK HTTP server bound to the local loopback interface exposes `POST /api/appointments` and `DELETE /api/appointments/{id}`. A React UI built with Vite calls those endpoints through the Vite development proxy. Booking rejects invalid arguments with `ValidationException` and conflicts with `ConflictException`. Cancellation reports `CANCELLED`, `NOT_FOUND`, or `ALREADY_CANCELLED`.

## Booking invariant

For any provider, active appointments never contain overlapping intervals; half-open intervals `[start, end)` allow one appointment to end exactly when the next begins.

## Data model and storage

An `Appointment` has a generated ID, provider ID, UTC `Instant` start and end, and an `ACTIVE` or `CANCELLED` status. The service stores active appointments by provider for conflict checks and stores all appointments by ID so cancellation can distinguish a missing appointment from one already cancelled. Cancellation marks the record and removes it from the provider's active list.

## Concurrency and validation

Input validation and end-time calculation happen before any store mutation. A single lock serializes conflict checking, booking, and cancellation, so concurrent calls cannot both claim an overlapping slot. The overlap check is `start < existingEnd && existingStart < end`.

## Assumptions and limitations

The service operates on UTC instants only, uses an in-memory store, and does not persist data across restarts or coordinate across service instances. The small local server is for local demonstration and does not include authentication, notifications, or recurring appointments. The simple per-operation lock favors clarity over parallel throughput.
