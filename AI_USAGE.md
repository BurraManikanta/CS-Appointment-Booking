# AI assistance record

## Engineering activity

AI assistance supported implementation of the appointment-booking service and creation of a lightweight browser interface backed by the Java API. The project requirements and final engineering decisions were reviewed against the acceptance criteria.

## Prompts

The original requests were provided as a detailed implementation checklist and short follow-up instructions. The following are professional summaries of those requests, not verbatim quotations:

1. **Appointment-booking service:** Implement a Java appointment-booking service using UTC `Instant` values and positive `Duration` values. Prevent overlapping active appointments for the same provider while allowing adjacent appointments and concurrent bookings to be handled safely. Support cancellation with clear results, reject invalid input without changing stored data, and provide focused automated tests and project documentation.
2. **Browser interface:** Add a small user interface that can book and cancel appointments by calling the service endpoints.

## AI contribution

AI assistance produced an in-memory Java service, appointment model, validation and conflict exceptions, cancellation results, and JUnit tests. It also implemented a lightweight HTML/CSS/JavaScript interface served by a local Java HTTP server.

## Review and decisions

The implementation was reviewed against the requested behavior. The service uses half-open intervals `[start, end)`, checks conflicts only against active appointments for the same provider, and performs booking and cancellation under a shared lock. Cancellation retains appointment records by ID so repeated cancellation can return `ALREADY_CANCELLED`. Validation and appointment end-time calculation occur before store mutation.

The UI is intentionally dependency-free and served by the Java application. The backend accepts UTC timestamps ending in `Z`; appointment data is held in memory and is lost when the process stops. The project does not include authentication, persistence, or a production deployment configuration.

## Verification

The automated test suite covers successful booking, same-provider overlaps, adjacent appointments, provider isolation, cancellation and rebooking, missing and repeated cancellation, invalid inputs, and end-time overflow. Verification performed:

- `.\gradlew.bat test` — all 12 tests passed.
- Manual HTTP checks — the UI returned HTTP 200; booking and cancellation succeeded; a non-UTC timestamp was rejected with HTTP 400.
