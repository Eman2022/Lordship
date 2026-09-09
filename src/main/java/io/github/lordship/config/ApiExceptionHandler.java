package io.github.lordship.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;


@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<Map<String, String>> of(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    // @Valid on a request body failed
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return of(HttpStatus.BAD_REQUEST, message.isBlank() ? "Request body is not valid" : message);
    }

    // malformed JSON, or a value the body cannot be read into
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> unreadableBody(HttpMessageNotReadableException e) {
        return of(HttpStatus.BAD_REQUEST, "Request body could not be read");
    }

    // a path variable or query parameter of the wrong type, e.g. a uuid that is not one
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, String>> badParameterType(MethodArgumentTypeMismatchException e) {
        return of(HttpStatus.BAD_REQUEST, e.getName() + " is not a valid " +
                (e.getRequiredType() == null ? "value" : e.getRequiredType().getSimpleName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<Map<String, String>> missingParameter(MissingServletRequestParameterException e) {
        return of(HttpStatus.BAD_REQUEST, "Missing required parameter: " + e.getParameterName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return of(HttpStatus.BAD_REQUEST, String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return of(HttpStatus.CONFLICT, String.valueOf(e.getMessage()));
    }
}
