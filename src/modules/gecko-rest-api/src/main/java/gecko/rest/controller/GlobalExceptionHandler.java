package gecko.rest.controller;

import gecko.rest.service.CircuitPreparationException;
import gecko.core.preparation.Diagnostic;
import gecko.core.preparation.ReadinessReport;
import java.util.List;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API.
 * Provides consistent error response format across all endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle ResponseStatusException from service layer.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex,
            WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", ex.getStatusCode().value());
        response.put("error", ex.getStatusCode().toString());
        response.put("message", ex.getReason());
        if (ex instanceof CircuitPreparationException preparation) {
            response.put("readyForHeadless", preparation.report().readyForHeadless());
            response.put("outcome", preparation.report().outcome());
            response.put("diagnostics", preparation.report().diagnostics());
        }

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * Handle validation errors from @Valid annotation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("message", "Invalid request parameters");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        response.put("fieldErrors", fieldErrors);
        response.put("readyForHeadless", false);
        response.put("outcome", ReadinessReport.Outcome.INVALID);
        response.put("diagnostics", fieldErrors.entrySet().stream().map(entry -> new Diagnostic(
                entry.getKey().equals("simulationTime") || entry.getKey().equals("timeStep") ? "INVALID_SETTING" : "INVALID_REQUEST",
                Diagnostic.Category.CONFIGURATION, Diagnostic.Severity.ERROR, entry.getValue(),
                null, null, entry.getKey(), null)).toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maps an unreadable JSON body to HTTP 400 with a typed parsing diagnostic.
     *
     * @param ex the request decoding failure.
     * @param request the web request context.
     * @return the error response with a MALFORMED_REQUEST diagnostic.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(HttpMessageNotReadableException ex, WebRequest request) {
        var report = new ReadinessReport(ReadinessReport.Outcome.INVALID,
                List.of(new Diagnostic("MALFORMED_REQUEST", Diagnostic.Category.PARSING, Diagnostic.Severity.ERROR,
                        "The JSON request cannot be read.", null, null, null, null)));
        return handleResponseStatusException(new CircuitPreparationException(HttpStatus.BAD_REQUEST, report), request);
    }

    /**
     * Handle general exceptions.
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidQuery(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, WebRequest request) {
        return handleResponseStatusException(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid query parameter: " + ex.getName()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex,
            WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");
        LOGGER.error("Unhandled exception processing request {}", path, ex);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        response.put("path", path);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        return handleResponseStatusException(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Your role does not permit this operation."), request);
    }

    /**
     * Handle 404 errors.
     */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            org.springframework.web.servlet.NoHandlerFoundException ex,
            WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", "Resource not found: " + ex.getRequestURL());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
