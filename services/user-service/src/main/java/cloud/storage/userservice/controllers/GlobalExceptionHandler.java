package cloud.storage.userservice.controllers;

import cloud.storage.userservice.customExceptions.AccessDeniedException;
import cloud.storage.userservice.customExceptions.FolderAlreadyExistsException;
import cloud.storage.userservice.customExceptions.FolderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ====================== ОЖИДАЕМЫЕ ОШИБКИ ======================

    @ExceptionHandler(FolderAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleFolderAlreadyExists(FolderAlreadyExistsException ex) {
        log.warn("Folder already exists at {}: {}", Instant.now(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(FolderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFolderNotFound(FolderNotFoundException ex) {
        log.warn("Folder not found at {}: {}", Instant.now(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied at {}: {}", Instant.now(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ====================== НЕОЖИДАННЫЕ ОШИБКИ ======================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        log.error("Unexpected error at {}: {}", Instant.now(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred: " + ex.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException ex) {
        log.warn("Endpoint not found at {}: {}", Instant.now(), ex.getRequestURL());
        return buildResponse(HttpStatus.NOT_FOUND, "Endpoint not found: " + ex.getRequestURL());
    }

    // ====================== ВСПОМОГАТЕЛЬНЫЙ МЕТОД ======================

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
