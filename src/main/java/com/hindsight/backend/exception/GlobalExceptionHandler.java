package com.hindsight.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:development}")
    private String environment;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, errors, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Internal Server Error: ", ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Throwable ex, HttpStatus status, String message, HttpServletRequest request) {
        boolean isProduction = "production".equalsIgnoreCase(environment);

        ErrorResponse.RequestDetails.RequestDetailsBuilder requestBuilder = ErrorResponse.RequestDetails.builder()
                .method(request.getMethod())
                .url(request.getRequestURI());

        if (!isProduction) {
            requestBuilder.ip(request.getRemoteAddr());
        }

        Map<String, String> trace = null;
        if (!isProduction) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            trace = new HashMap<>();
            trace.put("error", sw.toString());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .statuscode(status.value())
                .request(requestBuilder.build())
                .message(message)
                .data(null)
                .trace(trace)
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }
}
