package com.booking;

import com.booking.exception.ConflictException;
import com.booking.exception.ValidationException;
import com.booking.model.Appointment;
import com.booking.service.AppointmentBookingService;
import com.booking.service.CancelResult;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppointmentBookingApplication {
    private static final Logger LOGGER = Logger.getLogger(AppointmentBookingApplication.class.getName());
    private static final int MAX_REQUEST_BYTES = 16_384;

    private final AppointmentBookingService bookingService = new AppointmentBookingService();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        AppointmentBookingApplication application = new AppointmentBookingApplication();
        server.createContext("/", application::handle);
        server.start();
        System.out.println("Appointment booking API: http://localhost:" + port);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                route(exchange);
            } catch (ValidationException | DateTimeParseException | ArithmeticException exception) {
                sendError(exchange, 400, exception.getMessage());
            } catch (ConflictException exception) {
                sendError(exchange, 409, exception.getMessage());
            } catch (Exception exception) {
                LOGGER.log(Level.SEVERE, "Request failed", exception);
                sendError(exchange, 500, "Internal server error");
            }
        }
    }

    private void route(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/api/appointments")
                && exchange.getRequestMethod().equals("POST")) {
            book(exchange);
            return;
        }

        String appointmentPrefix = "/api/appointments/";
        if (path.startsWith(appointmentPrefix)
                && exchange.getRequestMethod().equals("DELETE")) {
            cancel(exchange, path.substring(appointmentPrefix.length()));
            return;
        }

        sendError(exchange, 404, "Endpoint not found");
    }

    private void book(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) {
            sendError(exchange, 413, "Request body is too large");
            return;
        }

        BookingRequest request;
        try {
            request = objectMapper.readValue(body, BookingRequest.class);
        } catch (JsonProcessingException exception) {
            throw new ValidationException("Request body must be valid JSON", exception);
        }
        if (request.start() == null || request.durationSeconds() == null) {
            throw new ValidationException("start and durationSeconds are required");
        }
        if (!request.start().endsWith("Z")) {
            throw new ValidationException("start must be a UTC timestamp ending in Z");
        }

        Instant start = Instant.parse(request.start());
        Duration duration = Duration.ofSeconds(request.durationSeconds());
        Appointment appointment = bookingService.book(request.providerId(), start, duration);
        sendJson(exchange, 201, appointment);
    }

    private void cancel(HttpExchange exchange, String appointmentId) throws IOException {
        if (appointmentId.isBlank() || appointmentId.contains("/")) {
            throw new ValidationException("appointmentId must not be blank");
        }

        CancelResult result = bookingService.cancel(appointmentId);
        sendJson(exchange, 200, new CancelResponse(result));
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] content = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, content.length);
        exchange.getResponseBody().write(content);
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, new ErrorResponse(message == null ? "Invalid request" : message));
    }

    public record BookingRequest(String providerId, String start, Long durationSeconds) {
    }

    private record CancelResponse(CancelResult result) {
    }

    private record ErrorResponse(String error) {
    }
}
